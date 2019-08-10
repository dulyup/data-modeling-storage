package com.neu.demo.repository;

import com.neu.demo.service.DataModelingService;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Map;

/**
 * @author lyupingdu
 * @date 2019-07-24.
 */
@Repository
public class PlanRepository {

    private final DataModelingService dataModelingService;

    public PlanRepository(DataModelingService dataModelingService) {
        this.dataModelingService = dataModelingService;
    }

    public Map<String, Object> findById(String id) {
        return dataModelingService.getById(id);
    }

    public String addNew(String content) throws IOException {
        return dataModelingService.addNew(content);
    }

    public void partialUpdateById(String id, String content) throws IOException {
        dataModelingService.partialUpdateById(id, content);
    }

    public void fullUpdateById(String id, String content) throws IOException {
        dataModelingService.fullUpdateById(id, content);
    }

    public void deleteById(String id) {
        dataModelingService.deleteById(id);
    }

    public Map<String, Object> getAll() {
        return dataModelingService.getAll();
    }
}
