
package music.facade;

import music.builder.Playlist;
import music.builder.PlaylistBuilder;
import music.model.MusicCatalog;
import music.model.Track;
import music.observer.Observer;
import music.player.AudioEngine;
import music.strategy.PlaybackStrategy;
import music.strategy.SequentialStrategy;
import music.data.PlaylistStorage;
import music.adapter.TrackImporter;
import music.adapter.YtDlpAudioAdapter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.file.Path;

public class MusicLibraryFacade {

    private final MusicCatalog catalog;
    private final AudioEngine audioEngine;

    // Observer Pattern
    private final List<Observer> observers =
            new CopyOnWriteArrayList<>();

    private volatile Track currentTrack;

    // Strategy Pattern
    private PlaybackStrategy strategy =
            new SequentialStrategy();

    private Playlist activePlaylist;
    private int currentTrackIndex = -1;

    private final TrackImporter youtubeImporter =
            new YtDlpAudioAdapter();

    // Periodically send playback position to observers.
    private final ScheduledExecutorService lyricsTimer =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "Lyrics-Timer");
                thread.setDaemon(true);
                return thread;
            });

    public MusicLibraryFacade(MusicCatalog catalog) {
        this.catalog = catalog;
        this.audioEngine = new AudioEngine();

        this.audioEngine.setOnTrackFinished(
                this::handleFinishedTrack
        );

        lyricsTimer.scheduleAtFixedRate(
                this::updateLyrics,
                200,
                200,
                TimeUnit.MILLISECONDS
        );
    }

    private synchronized void handleFinishedTrack(long playbackId) {

        // Ignore events from old or manually stopped tracks.
        if (!audioEngine.isCurrentPlayback(playbackId)) {
            return;
        }

        // Auto Next works only while playing a playlist.
        if (activePlaylist == null) {
            return;
        }

        System.out.println();
        System.out.println("Track finished. Playing next...");

        // Strategy decides which track comes next.
        playNextInPlaylist();
    }

    // ===========================
    // Play a single track
    // ===========================

    public synchronized void playTrack(String title) {
        Track track = catalog.findTrackByTitle(title);

        if (track == null) {
            System.out.println("Track not found: " + title);
            return;
        }

        if (playFile(track)) {
            activePlaylist = null;
            currentTrackIndex = -1;
        }
    }

    // ===========================
    // Builder Pattern integration
    // ===========================

    public void buildAndSavePlaylist(
            String name, List<String> titles) {

        PlaylistBuilder builder =
                new PlaylistBuilder().setName(name);

        for (String title : titles) {
            Track track = catalog.findTrackByTitle(title);

            if (track != null) {
                builder.addTrack(track);
            }
        }

        try {
            Playlist playlist = builder.build();

            catalog.savePlaylist(playlist);

            // Save playlist permanently.
            try {
                PlaylistStorage.save(playlist);
                System.out.println(
                        "Playlist saved successfully: " + name
                );
            } catch (IOException e) {
                System.out.println(
                        "Playlist saved only for this session: "
                                + e.getMessage()
                );
            }

        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }


    // ===========================
    // Strategy Pattern
    // ===========================

    public synchronized void setPlaybackStrategy(
            PlaybackStrategy strategy) {

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Strategy cannot be null"
            );
        }

        this.strategy = strategy;

        System.out.println(
                "Playback strategy: " +
                        strategy.getClass().getSimpleName()
        );
    }

    public synchronized void playPlaylist(String name) {
        Playlist playlist = catalog.findPlaylistByName(name);

        if (playlist == null) {
            System.out.println("Playlist not found: " + name);
            return;
        }

        activePlaylist = playlist;
        currentTrackIndex = -1;

        System.out.println("Starting playlist: " + name);

        playNextInPlaylist();
    }

    public synchronized void playNextInPlaylist() {
        if (activePlaylist == null ||
                activePlaylist.getTracks().isEmpty()) {

            System.out.println("No active playlist.");
            return;
        }

        List<Track> tracks = activePlaylist.getTracks();

        // Delegate selection to the current Strategy.
        int nextIndex = strategy.nextIndex(
                currentTrackIndex,
                tracks.size()
        );

        Track nextTrack = tracks.get(nextIndex);

        if (playFile(nextTrack)) {
            currentTrackIndex = nextIndex;
        }
    }

    // ===========================
    // Observer Pattern
    // ===========================

    public void registerObserver(Observer observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    private void notifyObservers(
            Track track, long positionMillis) {

        for (Observer observer : observers) {
            observer.update(track, positionMillis);
        }
    }

    private void updateLyrics() {
        Track track = currentTrack;

        if (track == null || !audioEngine.isActive()) {
            return;
        }

        try {
            long position = audioEngine.getPositionMillis();

            if (track == currentTrack) {
                notifyObservers(track, position);
            }
        } catch (Exception e) {
            System.err.println(
                    "Lyrics update error: " + e.getMessage()
            );
        }
    }

    // ===========================
    // Audio Engine integration
    // ===========================

    private boolean playFile(Track track) {
        try {
            currentTrack = null;

            audioEngine.play(track.getFilePath());

            currentTrack = track;

            // Observer must receive the new track immediately.
            notifyObservers(track, 0);

            System.out.println(
                    "Now playing: " + track.getTitle()
            );

            return true;

        } catch (Exception e) {
            System.err.println(
                    "[Facade] Playback error: " + e
            );
            e.printStackTrace(System.err);
            return false;
        }
    }

    public void pause() {
        audioEngine.pause();
    }

    public void resume() {
        audioEngine.resume();
    }

    public synchronized void stop() {
        currentTrack = null;
        audioEngine.stop();
    }

    public void setEcho(boolean enabled) {
        audioEngine.setEcho(enabled);
    }

    public void setFade(boolean enabled) {
        audioEngine.setFade(enabled);
    }

    public void close() {
        stop();
        lyricsTimer.shutdownNow();
    }

    public synchronized void playLocalFile(Path path) {

        Track track = new Track(
                path.getFileName().toString(),
                "Local File",
                path,
                ""
        );

        if (playFile(track)) {
            activePlaylist = null;
            currentTrackIndex = -1;
        }
    }

    public synchronized void importFromYouTube(String url) {
        try {
            Track track = youtubeImporter.importTrack(url);

            if (catalog.findTrackByTitle(track.getTitle()) == null) {
                catalog.addTrack(track);
            }

            System.out.println(
                    "YouTube track imported: " + track.getTitle()
            );

            // Play downloaded audio using the existing player.
            playLocalFile(track.getFilePath());

        } catch (IOException e) {
            System.err.println(
                    "Download error: " + e.getMessage()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Download interrupted.");
        }
    }

    public void setVolume(int percent) {
        audioEngine.setVolume(percent);
    }

    public int getVolume() {
        return audioEngine.getVolume();
    }

}