package com.example.mangaapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.mangaapp.fragments.account.AccountFragment;
import com.example.mangaapp.API_MangaDex.Chapter;
import com.example.mangaapp.API_MangaDex.MangaDetail;

/**
 * Утиліта для управління прогресом читання та інтеграції з MangaDex API
 */
public class ReadingProgressManager {
    
    private static final String TAG = "ReadingProgressManager";
    private static final String PREFS_NAME = "ReadingProgressPrefs";
    
    private Context context;
    private AccountFragment accountFragment;
    
    public ReadingProgressManager(Context context, AccountFragment accountFragment) {
        this.context = context;
        this.accountFragment = accountFragment;
    }
    
    /**
     * Оновлює прогрес читання з даними з MangaDex
     */
    public void updateProgressFromMangaDex(String mangaId, String chapterId, 
                                         MangaDetail mangaDetail, Chapter chapter,
                                         int currentPage, int totalPages) {
        
        if (accountFragment == null) {
            Log.w(TAG, "AccountFragment is null, cannot update progress");
            return;
        }
        
        try {
            // Отримуємо назву манги
            String mangaTitle = getMangaTitle(mangaDetail);
            
            // Отримуємо інформацію про главу
            String chapterTitle = getChapterTitle(chapter);
            String chapterNumber = getChapterNumber(chapter);
            
            // Отримуємо URL обкладинки
            String coverUrl = getCoverUrl(mangaDetail);
            
            Log.d(TAG, String.format("Updating progress: %s - %s, page %d/%d", 
                mangaTitle, chapterTitle, currentPage, totalPages));
            
            // Оновлюємо прогрес через AccountFragment
            accountFragment.updateReadingProgress(
                mangaId, chapterId, mangaTitle, chapterTitle, 
                chapterNumber, coverUrl, currentPage, totalPages
            );
            
            // Зберігаємо локально для офлайн доступу
            saveProgressLocally(mangaId, chapterId, currentPage, totalPages);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating reading progress", e);
        }
    }
    
    /**
     * Отримує назву манги з MangaDetail
     */
    private String getMangaTitle(MangaDetail mangaDetail) {
        if (mangaDetail != null && mangaDetail.getData() != null && 
            mangaDetail.getData().getAttributes() != null) {
            
            MangaDetail.Attributes attributes = mangaDetail.getData().getAttributes();
            if (attributes.getTitle() != null) {
                // Спочатку намагаємося отримати українську назву
                String title = attributes.getTitle().get("uk");
                if (title == null || title.isEmpty()) {
                    title = attributes.getTitle().get("en");
                }
                if (title == null || title.isEmpty()) {
                    // Беремо першу доступну назву
                    for (String t : attributes.getTitle().values()) {
                        if (t != null && !t.isEmpty()) {
                            title = t;
                            break;
                        }
                    }
                }
                return title != null ? title : "Без назви";
            }
        }
        return "Без назви";
    }
    
    /**
     * Отримує назву глави з Chapter
     */
    private String getChapterTitle(Chapter chapter) {
        if (chapter != null && chapter.getData() != null && 
            chapter.getData().getAttributes() != null) {
            
            Chapter.Attributes attributes = chapter.getData().getAttributes();
            if (attributes.getTitle() != null && !attributes.getTitle().isEmpty()) {
                return attributes.getTitle();
            }
        }
        return "";
    }
    
    /**
     * Отримує номер глави з Chapter
     */
    private String getChapterNumber(Chapter chapter) {
        if (chapter != null && chapter.getData() != null && 
            chapter.getData().getAttributes() != null) {
            
            Chapter.Attributes attributes = chapter.getData().getAttributes();
            if (attributes.getChapter() != null && !attributes.getChapter().isEmpty()) {
                return attributes.getChapter();
            }
        }
        return "1";
    }
    
    /**
     * Отримує URL обкладинки з MangaDetail
     */
    private String getCoverUrl(MangaDetail mangaDetail) {
        if (mangaDetail != null && mangaDetail.getData() != null && 
            mangaDetail.getData().getRelationships() != null) {
            
            for (com.example.mangaapp.API_MangaDex.MangaResponse.Relationship rel : 
                 mangaDetail.getData().getRelationships()) {
                
                if ("cover_art".equals(rel.getType()) && rel.getAttributes() != null) {
                    String fileName = rel.getAttributes().getFileName();
                    if (fileName != null && !fileName.isEmpty()) {
                        return "https://uploads.mangadex.org/covers/" + 
                               mangaDetail.getData().getId() + "/" + fileName;
                    }
                }
            }
        }
        return "";
    }
    
    /**
     * Зберігає прогрес локально
     */
    private void saveProgressLocally(String mangaId, String chapterId, int currentPage, int totalPages) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        String key = mangaId + "_" + chapterId;
        editor.putInt(key + "_current", currentPage);
        editor.putInt(key + "_total", totalPages);
        editor.putLong(key + "_timestamp", System.currentTimeMillis());
        
        editor.apply();
        Log.d(TAG, "Progress saved locally: " + key);
    }
    
    /**
     * Отримує збережений прогрес
     */
    public int[] getLocalProgress(String mangaId, String chapterId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = mangaId + "_" + chapterId;
        
        int currentPage = prefs.getInt(key + "_current", 0);
        int totalPages = prefs.getInt(key + "_total", 0);
        
        return new int[]{currentPage, totalPages};
    }
}
