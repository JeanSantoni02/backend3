package com.bank.xyz.batch.partitioner;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TransaccionPartitioner implements Partitioner {

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();

        // Simulamos 4 particiones para procesamiento paralelo
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putInt("partitionId", i);
            context.putInt("startId", i * 1000);
            context.putInt("endId", (i + 1) * 1000);
            partitions.put("particion_" + i, context);
        }

        return partitions;
    }
}