# ssrf-guard-demo

**English** · [한국어](README.ko.md)

Runnable example for [`ssrf-guard`](https://github.com/devslab-kr/ssrf-guard) — SSRF (Server-Side Request Forgery) protection for the JVM.

One Spring Boot app shows **all three Spring HTTP clients** wired through the same `UrlPolicy`:

- `RestClient` (Spring 6.1+) via the meta `kr.devslab:ssrf-guard:3.0.1` artifact
- `RestTemplate` via `kr.devslab:ssrf-guard-resttemplate:3.0.1`
- `WebClient` (WebFlux) via `kr.devslab:ssrf-guard-webclient:3.0.1`

Plus a `/attacks` endpoint that lists every SSRF bypass pattern the guard catches, with copy-paste curls for each.

## Prerequisites

- JDK 21+
- An internet-reachable host that's whitelisted (`httpbin.org` by default — used to show the allowed path actually reaches the network).

## Run

```bash
cd ssrf-guard-demo
./gradlew bootRun
```

App comes up on `http://localhost:8080`.

## Try it

### Allowed — RestClient hits the real httpbin.org

```bash
curl 'http://localhost:8080/fetch?url=https://httpbin.org/get' | jq
```

```json
{
  "status": "allowed",
  "client": "RestClient",
  "url": "https://httpbin.org/get",
  "bodyPreview": "{\n  \"args\": {}, \n  \"headers\": { ... }, \n  ..."
}
```

### Blocked — AWS metadata theft attempt (the canonical SSRF→cloud-takeover)

```bash
curl 'http://localhost:8080/fetch?url=http://169.254.169.254/latest/meta-data/iam/security-credentials/' | jq
```

```json
{
  "status": "blocked",
  "client": "RestClient",
  "url": "http://169.254.169.254/latest/meta-data/iam/security-credentials/",
  "reason": "blocked_ip_literal",
  "message": "IP-literal host blocked (rejectIpLiteralHosts=true): 169.254.169.254"
}
```

### Blocked — host not in the whitelist

```bash
curl 'http://localhost:8080/fetch?url=https://evil.com/' | jq
# reason: "blocked_host"
```

### Blocked — decimal-encoded loopback (`2130706433` == `127.0.0.1`)

```bash
curl 'http://localhost:8080/fetch?url=http://2130706433/' | jq
# reason: "blocked_ip_literal"
```

### Blocked — userinfo (`user:pass@host`)

```bash
curl 'http://localhost:8080/fetch?url=https://user:pass@httpbin.org/get' | jq
# reason: "blocked_userinfo"
```

### Blocked — redirect to AWS metadata (4th defense layer)

```bash
curl 'http://localhost:8080/fetch?url=https://httpbin.org/redirect-to?url=http://169.254.169.254/' | jq
# Caught at hop 2 — even though the initial host is whitelisted, the redirect
# strategy re-validates every URL change.
```

### Same attacks, same outcome, different HTTP client

```bash
curl 'http://localhost:8080/fetch-resttemplate?url=http://169.254.169.254/'
curl 'http://localhost:8080/fetch-webclient?url=http://169.254.169.254/'
```

Identical `reason` field — all three HTTP clients are wrapped by the same `UrlPolicy` bean.

### The full attack matrix (15 entries)

```bash
curl http://localhost:8080/attacks | jq
```

Returns every attack pattern with its `expectedReason` and pre-built `tryRestClient` / `tryRestTemplate` / `tryWebClient` curl strings. Pipe a single one into `bash`:

```bash
curl -s http://localhost:8080/attacks \
  | jq -r '.attacks[] | select(.name == "aws-metadata-credentials") | .tryRestClient' \
  | bash | jq
```

### Observability — Micrometer metrics

```bash
# After running a few of the curls above:
curl -s http://localhost:8080/actuator/metrics/ssrf_guard_blocked_total | jq
curl -s http://localhost:8080/actuator/prometheus | grep ssrf_guard
```

You'll see counters per `reason` tag (`blocked_host`, `blocked_ip_literal`, `blocked_private_ip`, `blocked_userinfo`, `blocked_redirect`, ...) and a separate `ssrf_guard_allowed_total` for the requests that passed.

## What to read

| File | Why |
| --- | --- |
| `build.gradle.kts` | The only dependencies beyond the standard starters are `kr.devslab:ssrf-guard:3.0.1`, `kr.devslab:ssrf-guard-resttemplate:3.0.1`, `kr.devslab:ssrf-guard-webclient:3.0.1` — no manual configuration class needed |
| `application.yml` | Every `ssrf.guard.*` knob in one place with comments |
| `web/FetchController.java` | The whole RestClient story — three lines of setup, the guard runs invisibly |
| `web/FetchResttemplateController.java` | Same shape for RestTemplate — no migration needed for legacy code |
| `web/FetchWebClientController.java` | Reactive variant; demonstrates `SsrfGuardException` flowing through `Mono.onErrorResume` |
| `web/AttackDemoController.java` | Catalog of attack patterns + expected `BlockReason` for each |

## Loosening the whitelist (sanity check)

Want to confirm the guard is actually doing anything? Edit `application.yml`:

```yaml
ssrf:
  guard:
    enabled: false
```

Restart and run the AWS metadata curl again — it'll now actually hit `169.254.169.254` (and time out on most networks, but the guard isn't intervening anymore).

## Verify the build

```bash
./gradlew build
```

Runs the smoke test in `SsrfGuardDemoApplicationTests`, which boots the app and asserts:

1. A whitelisted URL passes through (`status=allowed`),
2. An attack URL is blocked with the right `reason` tag,
3. The actuator metrics endpoint exposes `ssrf_guard_blocked_total` after the block.

## Further reading

- ssrf-guard docs site: <https://ssrf-guard.devslab.kr/>
- ssrf-guard repo: <https://github.com/devslab-kr/ssrf-guard>
- Java SSRF training (attack patterns referenced by `AttackDemoController`): <https://github.com/JoyChou93/java-sec-code>
- OWASP SSRF top 10: <https://owasp.org/Top10/A10_2021-Server-Side_Request_Forgery_%28SSRF%29/>
