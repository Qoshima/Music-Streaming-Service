package music.facade;

import music.model.Track;
import music.model.MusicCatalog;
import music.builder.Playlist;
import music.builder.PlaylistBuilder;
import music.observer.Observer;
import music.strategy.PlaybackStrategy;
import music.player.AudioEngine;

import java.util.ArrayList;
import java.util.List;

public class MusicLibraryFacade {
    private final MusicCatalog catalog;
    private final AudioEngine audioEngine;
    private final List<Observer> observers;
    private PlaybackStrategy strategy;
    
    private Playlist activePlaylist;
    private int currentTrackIndex;

    public MusicLibraryFacade(MusicCatalog catalog) {
        this.catalog = catalog;
        this.audioEngine = new AudioEngine();
        this.observers = new ArrayList<>();
    }

    public void playTrack(String title) {
        Track track = catalog.findTrackByTitle(title);
        if (track != null) {
            notifyObservers(track);
            playFile(track);
            this.activePlaylist = null;
        } else {
            System.out.println("Track not found: " + title);
        }
    }

    public void buildAndSavePlaylist(String name, List<String> trackTitles) {
        PlaylistBuilder builder = new PlaylistBuilder().setName(name);
        for (String title : trackTitles) {
            Track track = catalog.findTrackByTitle(title);
            if (track != null) {
                builder.addTrack(track);
            }
        }
        try {
            Playlist playlist = builder.build();
            catalog.savePlaylist(playlist);
            System.out.println("Playlist '" + name + "' saved with " + playlist.getTracks().size() + " tracks.");
        } catch (IllegalStateException e) {
            System.out.println("Error building playlist: " + e.getMessage());
        }
    }

    public void playPlaylist(String name) {
        Playlist playlist = catalog.findPlaylistByName(name);
        if (playlist != null) {
            this.activePlaylist = playlist;
            this.currentTrackIndex = 0;
            System.out.println("Starting playlist: " + name);
            playNextInPlaylist();
        } else {
            System.out.println("Playlist not found: " + name);
        }
    }

    public void playNextInPlaylist() {
        if (activePlaylist == null || activePlaylist.getTracks().isEmpty()) return;
        
        Track nextTrack;
        if (strategy != null) {
            nextTrack = strategy.nextTrack(activePlaylist.getTracks());
        } else {
            if (currentTrackIndex >= activePlaylist.getTracks().size()) currentTrackIndex = 0;
            nextTrack = activePlaylist.getTracks().get(currentTrackIndex++);
        }
        
        notifyObservers(nextTrack);
        playFile(nextTrack);
    }

    // --- Observer Management ---
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    private void notifyObservers(Track track) {
        for (Observer obs : observers) {
            obs.update(track);
        }
    }

    // --- Strategy Management ---
    public void setPlaybackStrategy(PlaybackStrategy strategy) {
        this.strategy = strategy;
    }

    private void playFile(Track track) {
        try {
            audioEngine.play(track.getFilePath());
        } catch (Exception e) {
            System.err.println("Playback error: " + e.getMessage());
        }
    }
    public void pause() { audioEngine.pause(); }
    public void resume() { audioEngine.resume(); }
    public void stop() { audioEngine.stop(); }
    public void setEcho(boolean enabled) { audioEngine.setEcho(enabled); }
    public void setFade(boolean enabled) { audioEngine.setFade(enabled); }
}