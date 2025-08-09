package org.phong.zenflow.workflow.subdomain.workflow_version.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.exception.WorkflowException;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.workflow_version.dto.WorkflowVersionDto;
import org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.mapstruct.WorkflowVersionMapper;
import org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.persistence.entity.WorkflowVersion;
import org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.persistence.repository.WorkflowVersionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WorkflowVersionService {

    private final WorkflowVersionRepository versionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionMapper versionMapper;

    private static final long CACHE_TTL_MS = 5000L;
    private static final int MAX_VERSIONS = 25;
    private static final int RETENTION_DAYS = 3;

    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    @Transactional
    public void autoSave(UUID workflowId, WorkflowDefinition definition) {
        String hash = definition != null ? String.valueOf(definition.hashCode()) : "null";
        long now = System.currentTimeMillis();

        CacheEntry entry = cache.get(workflowId);
        if (entry != null && entry.hash().equals(hash) && now - entry.timestamp() < CACHE_TTL_MS) {
            return; // Recently saved same content
        }

        Optional<WorkflowVersion> latestOpt = versionRepository.findTopByWorkflow_IdOrderByVersionDesc(workflowId);
        if (latestOpt.isPresent()) {
            WorkflowVersion latest = latestOpt.get();
            String latestHash = latest.getDefinition() != null ? String.valueOf(latest.getDefinition().hashCode()) : "null";
            if (latestHash.equals(hash)) {
                cache.put(workflowId, new CacheEntry(hash, now));
                return; // No change compared to latest version
            }
        }

        cache.put(workflowId, new CacheEntry(hash, now));

        Workflow workflowRef = workflowRepository.getReferenceById(workflowId);
        int nextVersion = latestOpt.map(v -> v.getVersion() + 1).orElse(1);

        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflow(workflowRef);
        version.setVersion(nextVersion);
        version.setDefinition(definition);
        version.setIsAutosave(true);

        versionRepository.save(version);
    }

    private record CacheEntry(String hash, long timestamp) {
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupAllVersions() {
        workflowRepository.findAll().forEach(w -> cleanupVersions(w.getId()));
    }

    private void cleanupVersions(UUID workflowId) {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(RETENTION_DAYS);
        versionRepository.deleteByWorkflow_IdAndCreatedAtBefore(workflowId, threshold);

        List<WorkflowVersion> versions = versionRepository.findByWorkflow_IdOrderByVersionDesc(workflowId);
        if (versions.size() > MAX_VERSIONS) {
            versionRepository.deleteAll(versions.subList(MAX_VERSIONS, versions.size()));
        }
    }

    public List<WorkflowVersionDto> getVersions(UUID workflowId) {
        return versionMapper.toDtoList(versionRepository.findByWorkflow_IdOrderByVersionDesc(workflowId));
    }

    public WorkflowVersionDto getVersion(UUID workflowId, Integer version) {
        return versionRepository.findByWorkflow_IdAndVersion(workflowId, version)
                .map(versionMapper::toDto)
                .orElseThrow(() -> new WorkflowException("Workflow version not found"));
    }

    @Transactional
    public void deleteVersion(UUID workflowId, Integer version) {
        WorkflowVersion existing = versionRepository.findByWorkflow_IdAndVersion(workflowId, version)
                .orElseThrow(() -> new WorkflowException("Workflow version not found"));
        versionRepository.delete(existing);
    }

    @Transactional
    public void deleteAllVersions(UUID workflowId) {
        versionRepository.deleteByWorkflow_Id(workflowId);
    }
}

