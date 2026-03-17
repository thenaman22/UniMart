import { useState } from 'react'

function getOrientation(width, height) {
  if (!width || !height) return 'landscape'
  const ratio = width / height
  if (ratio < 0.85) return 'portrait'
  if (ratio > 1.15) return 'landscape'
  return 'square'
}

export function ListingPreview({ listing, mode = 'feed' }) {
  const mediaItems = listing.media?.length
    ? listing.media
    : listing.previewMediaUrl
      ? [{ url: listing.previewMediaUrl, type: listing.previewMediaType }]
      : []
  const [activeIndex, setActiveIndex] = useState(0)
  const [orientations, setOrientations] = useState({})
  const isTileMode = mode === 'tile'

  if (mediaItems.length === 0) {
    return null
  }

  const safeIndex = isTileMode ? 0 : Math.min(activeIndex, mediaItems.length - 1)
  const activeMedia = mediaItems[safeIndex]
  const orientation = orientations[safeIndex] || 'landscape'
  const src = `http://localhost:8080${activeMedia.url}`
  const frameClassName = `listing-media-frame ${mode} ${orientation}`
  const mediaClassName = `listing-media${mode === 'feed' ? ' listing-media-feed' : ''}${isTileMode ? ' listing-media-tile' : ''}`

  function updateOrientation(index, width, height) {
    setOrientations(current => ({
      ...current,
      [index]: getOrientation(width, height)
    }))
  }

  return (
    <div className="listing-carousel">
      <div className={frameClassName}>
        {activeMedia.type === 'VIDEO' ? (
          <video
            className={mediaClassName}
            src={src}
            controls
            onLoadedMetadata={event => {
              const { videoWidth, videoHeight } = event.currentTarget
              updateOrientation(safeIndex, videoWidth, videoHeight)
            }}
          />
        ) : (
          <img
            className={mediaClassName}
            src={src}
            alt={listing.title}
            onLoad={event => {
              const { naturalWidth, naturalHeight } = event.currentTarget
              updateOrientation(safeIndex, naturalWidth, naturalHeight)
            }}
          />
        )}

        {mediaItems.length > 1 && !isTileMode && (
          <>
            <button
              type="button"
              className="carousel-control prev"
              aria-label="Previous media"
              onClick={() => setActiveIndex(current => (current - 1 + mediaItems.length) % mediaItems.length)}
            >
              ‹
            </button>
            <button
              type="button"
              className="carousel-control next"
              aria-label="Next media"
              onClick={() => setActiveIndex(current => (current + 1) % mediaItems.length)}
            >
              ›
            </button>
            <div className="carousel-dots" aria-label={`Media ${safeIndex + 1} of ${mediaItems.length}`}>
              {mediaItems.map((item, index) => (
                <button
                  key={`${item.url}-${index}`}
                  type="button"
                  className={`carousel-dot${index === safeIndex ? ' active' : ''}`}
                  aria-label={`Go to media ${index + 1}`}
                  onClick={() => setActiveIndex(index)}
                />
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
