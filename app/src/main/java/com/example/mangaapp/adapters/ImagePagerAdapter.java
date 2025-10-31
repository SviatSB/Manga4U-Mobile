// ImagePagerAdapter.java
package com.example.mangaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.GestureDetector;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mangaapp.R;
import com.example.mangaapp.fragments.main.ReaderFragment;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
    private List<String> imageUrls;
    private String readingMode;
    private static final float ZOOM_THRESHOLD = 1.1f;
    private GestureDetector gestureDetector;

    public ImagePagerAdapter(List<String> imageUrls, String readingMode) {
        this.imageUrls = imageUrls;
        this.readingMode = readingMode;
        Log.e("MangaApp", "[ImagePagerAdapter] Created with " + imageUrls.size() + " images, mode: " + readingMode);
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_page, parent, false);
        Log.e("MangaApp", "[ImagePagerAdapter] Created new ViewHolder");
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        PhotoView photoView = holder.imageView;
        Log.e("MangaApp", "[ImagePagerAdapter] Loading image " + position + ": " + imageUrl);

        // Configure PhotoView based on reading mode
        if ("vertical".equals(readingMode)) {
            // Вертикальний режим для манхви
            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            photoView.setMinimumScale(1.0f);
            photoView.setMaximumScale(3.0f);
            Log.e("MangaApp", "[ImagePagerAdapter] Set vertical mode for manhwa");
        } else {
            // Горизонтальний режим для манги
            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            photoView.setMinimumScale(1.0f);
            photoView.setMaximumScale(3.0f);
            Log.e("MangaApp", "[ImagePagerAdapter] Set horizontal mode for manga");
        }

        // Завантаження зображення
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .into(photoView);

        // Налаштування обробки дотиків
        photoView.setOnScaleChangeListener((scaleFactor, focusX, focusY) -> {
            if (scaleFactor > ZOOM_THRESHOLD) {
                photoView.setAllowParentInterceptOnEdge(false);
            } else {
                photoView.setAllowParentInterceptOnEdge(true);
            }
        });

        // Створюємо GestureDetector для обробки свайпів
        gestureDetector = new GestureDetector(holder.itemView.getContext(), 
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    
                    // Перевіряємо, чи це горизонтальний свайп
                    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 100) {
                        RecyclerView recyclerView = (RecyclerView) holder.itemView.getParent();
                        if (diffX > 0 && position > 0) {
                            // Свайп вправо - попередня сторінка
                            recyclerView.smoothScrollToPosition(position - 1);
                            return true;
                        } else if (diffX < 0 && position < getItemCount() - 1) {
                            // Свайп вліво - наступна сторінка
                            recyclerView.smoothScrollToPosition(position + 1);
                            return true;
                        }
                    }
                    return false;
                }
            });

        // Обробка дотиків
        photoView.setOnTouchListener((v, event) -> {
            if ("vertical".equals(readingMode)) {
                // У вертикальному режимі дозволяємо вертикальну прокрутку
                return false;
            } else {
                // У горизонтальному режимі обробляємо свайпи
                return gestureDetector.onTouchEvent(event);
            }
        });
        
        // Додаємо обробник дотиків по центру для показу/скриття меню
        photoView.setOnClickListener(v -> {
            // Використовуємо простіший підхід - передаємо callback через контекст
            if (v.getContext() instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) v.getContext();
                // Знаходимо ReaderFragment через FragmentManager
                androidx.fragment.app.FragmentManager fragmentManager = activity.getSupportFragmentManager();
                if (fragmentManager != null) {
                    for (androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {
                        if (fragment instanceof ReaderFragment) {
                            // Викликаємо метод через рефлексію
                            try {
                                java.lang.reflect.Method method = fragment.getClass().getDeclaredMethod("toggleNavigationMenu");
                                method.setAccessible(true);
                                method.invoke(fragment);
                            } catch (Exception e) {
                                // Ігноруємо помилки
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView imageView;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.page_image);
            Log.e("MangaApp", "[ImagePagerAdapter] ViewHolder initialized with PhotoView");
        }
    }
}