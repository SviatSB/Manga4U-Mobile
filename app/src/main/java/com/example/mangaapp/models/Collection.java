package com.example.mangaapp.models;

import java.util.List;
import java.util.ArrayList;

public class Collection {
    private String id;
    private String name;
    private boolean isSystem;
    private boolean isPublic;
    // Server returns list of manga DTOs (with name + externalId). Keep both representations for compatibility.
    private List<String> mangaIds;
    private List<MangaShortDto> mangas;
    private long createdAt;

    public Collection() {}

    public Collection(String id, String name, boolean isSystem) {
        this.id = id;
        this.name = name;
        this.isSystem = isSystem;
        this.isPublic = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public List<String> getMangaIds() { return mangaIds; }
    public void setMangaIds(List<String> mangaIds) { this.mangaIds = mangaIds; }

    public List<MangaShortDto> getMangas() { return mangas; }
    public void setMangas(List<MangaShortDto> mangas) { this.mangas = mangas; }

    // Helper to get external ids from either mangas DTO or raw mangaIds
    public List<String> getMangaExternalIds() {
        List<String> ids = new ArrayList<>();
        if (mangaIds != null && !mangaIds.isEmpty()) {
            ids.addAll(mangaIds);
        }
        if (mangas != null && !mangas.isEmpty()) {
            for (MangaShortDto m : mangas) {
                if (m != null && m.getExternalId() != null) ids.add(m.getExternalId());
            }
        }
        return ids;
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
