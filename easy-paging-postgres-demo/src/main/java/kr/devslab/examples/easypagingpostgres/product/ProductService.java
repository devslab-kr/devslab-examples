package kr.devslab.examples.easypagingpostgres.product;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductMapper mapper;

    public ProductService(ProductMapper mapper) {
        this.mapper = mapper;
    }

    public List<Product> findAll(String category) {
        return mapper.findAll(category);
    }
}
