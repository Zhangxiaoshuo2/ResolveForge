package dev.resolveforge.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/** Small, strict parser for repository-authored Markdown, not a general document ingestion system. */
@Component
public class KnowledgeLoader {
    private static final Pattern HEADING = Pattern.compile("^## (.+) \\{#([a-z0-9-]+)}$");
    public List<PolicyChunk> load() {
        try { return load(new PathMatchingResourcePatternResolver().getResources("classpath*:knowledge/*.md")); }
        catch (IOException ex) { throw new IllegalStateException("knowledge_read_failed", ex); }
    }
    public List<PolicyChunk> load(Resource[] resources) {
        List<PolicyChunk> chunks = new ArrayList<>();
        for (Resource resource : resources) {
            try { chunks.addAll(parse(resource.getContentAsString(StandardCharsets.UTF_8))); }
            catch (IOException ex) { throw new IllegalStateException("knowledge_read_failed", ex); }
        }
        if (chunks.isEmpty()) throw new IllegalStateException("knowledge_empty");
        Set<String> ids = new HashSet<>();
        for (PolicyChunk c : chunks) if (!ids.add(c.chunkId())) throw new IllegalStateException("duplicate_chunk_id");
        return chunks.stream().sorted(Comparator.comparing(PolicyChunk::chunkId)).toList();
    }
    public List<PolicyChunk> parse(String markdown) {
        String normalized = markdown.replace("\r\n", "\n").strip();
        String[] parts = normalized.split("\n---\n", 2);
        if (!normalized.startsWith("---\n") || parts.length != 2) throw new IllegalArgumentException("knowledge_metadata_required");
        Map<String,String> metadata = new HashMap<>();
        parts[0].substring(4).lines().forEach(line -> {
            String[] pair = line.split(":", 2);
            if (pair.length != 2) throw new IllegalArgumentException("knowledge_metadata_invalid");
            metadata.put(pair[0].trim(), pair[1].trim());
        });
        for (String field : List.of("sourceId", "version", "effectiveDate", "kind"))
            if (metadata.getOrDefault(field, "").isBlank()) throw new IllegalArgumentException("knowledge_metadata_missing_"+field);
        if (!"DEMO".equals(metadata.get("kind"))) throw new IllegalArgumentException("knowledge_must_be_demo");
        if (!metadata.get("sourceId").matches("[A-Z0-9-]+")) throw new IllegalArgumentException("invalid_source_id");
        List<PolicyChunk> chunks = new ArrayList<>();
        String title = null, anchor = null;
        StringBuilder body = new StringBuilder();
        for (String line : parts[1].split("\n")) {
            var match = HEADING.matcher(line);
            if (match.matches()) {
                if (title != null) addSection(chunks, metadata, normalized, title, anchor, body.toString());
                title = match.group(1); anchor = match.group(2); body.setLength(0);
            } else if (title != null) { body.append(line).append('\n'); }
        }
        if (title != null) addSection(chunks, metadata, normalized, title, anchor, body.toString());
        return chunks;
    }
    private void addSection(List<PolicyChunk> chunks, Map<String,String> meta, String document, String title, String anchor, String body) {
        StringBuilder group = new StringBuilder();
        int part = 1;
        for (String paragraph : body.strip().split("\\n\\s*\\n")) {
            if (paragraph.length()>700) throw new IllegalArgumentException("knowledge_paragraph_exceeds_700_chars");
            if (!group.isEmpty() && group.length()+paragraph.length()+2>700) {
                chunks.add(chunk(meta, document, title, anchor, part++, group.toString())); group.setLength(0);
            }
            if (!group.isEmpty()) group.append("\n\n");
            group.append(paragraph);
        }
        if (!group.toString().isBlank()) chunks.add(chunk(meta, document, title, anchor, part, group.toString()));
    }
    private PolicyChunk chunk(Map<String,String> m, String doc, String title, String anchor, int part, String text) {
        return new PolicyChunk(m.get("sourceId"), m.get("sourceId")+"#"+anchor+"-p"+part, title, text,
                m.get("version"), m.get("effectiveDate"), hash(doc), hash(title+"\n"+text));
    }
    public static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
