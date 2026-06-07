package com.backwell.api_service.modules.products.service;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.products.dto.CategoryNode;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;

@Component
public class CategoryGraphValidator {
    public void validate(List<CategoryNode> nodes) {

        Map<String, List<String>> adjacencyList = new HashMap<>();
        for (CategoryNode node : nodes) {
            if (node.parentName() != null){
                adjacencyList.computeIfAbsent(
                        node.parentName(),
                        k-> new ArrayList<>()).add(node.name());
            }
        }

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (var node : nodes) {
            if (hasCycle(node.name(), adjacencyList, visiting, visited)) {
                String msg = String.format("Ciclo detectado en el grafo de categorías: `%s`", node.name());
                throw new BusinessException(msg, INVALID_CATEGORY_GRAPH);
            }
        }
    }

    private boolean hasCycle(
            String current,
            Map<String,List<String>> adj,
            Set<String> visiting,
            Set<String> visited
    ) {
        if (visiting.contains(current)) return true; // ¡CICLO!
        if (visited.contains(current)) return false; // Ya validado

        visiting.add(current);

        List<String> children = adj.getOrDefault(current, Collections.emptyList());
        for (String child : children) {
            if (hasCycle(child, adj, visiting, visited)) return true;
        }

        visiting.remove(current);
        visited.add(current);
        return false;
    }
}
