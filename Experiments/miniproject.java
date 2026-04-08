package Experiments;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

// --- DATA CLASSES ---
abstract class AudioTrack implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id, title, artist;
    private boolean isFavorite = false;

    public AudioTrack(String title, String artist) {
        this.id = UUID.randomUUID().toString().substring(0, 5);
        this.title = title;
        this.artist = artist;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean f) { this.isFavorite = f; }
    public abstract String getType();
    public abstract String getSubInfo();
}

class Song extends AudioTrack {
    private String language, genre;
    public Song(String t, String a, String l, String g) {
        super(t, a); this.language = l; this.genre = g;
    }
    @Override public String getType() { return "🎵 Song"; }
    @Override public String getSubInfo() { return "🎸 " + genre + " • 🌍 " + language; }
}

class Podcast extends AudioTrack {
    private int duration, episode;
    public Podcast(String t, String h, int d, int e) {
        super(t, h); this.duration = d; this.episode = e;
    }
    @Override public String getType() { return "🎙️ Podcast"; }
    @Override public String getSubInfo() { return "🎬 Episode " + episode + " • ⏱️ " + (duration/60) + " min"; }
}

// --- MAIN SERVER ---
public class miniproject {
    private static List<AudioTrack> playlist = new ArrayList<>();
    private static final String FILE = "music_bridge.dat";

    public static void main(String[] args) throws Exception {
        load();
        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);
        
        server.createContext("/", (t) -> {
            String html = "<html><head><meta charset='UTF-8'><style>" +
                "body{font-family:'Segoe UI',sans-serif;margin:0;display:flex;background:#121212;color:white;}" +
                ".sidebar{width:240px;background:#000;height:100vh;padding:20px;position:fixed;border-right:1px solid #282828;}" +
                ".content{margin-left:280px;padding:40px;width:calc(100% - 320px);}" +
                ".nav-btn{display:block;width:100%;padding:12px;margin-bottom:8px;background:none;border:none;color:#b3b3b3;text-align:left;cursor:pointer;font-weight:bold;font-size:16px;}" +
                ".nav-btn:hover{color:white;background:#282828;border-radius:4px;}" +
                ".page{display:none;}.active-page{display:block;}" +
                "input{display:block;width:100%;max-width:400px;padding:12px;margin:15px 0;border:1px solid #333;border-radius:4px;background:#282828;color:white;}" +
                "button.action{background:#1DB954;color:white;border:none;padding:12px 30px;border-radius:25px;cursor:pointer;font-weight:bold;}" +
                ".track-card{background:#181818;padding:15px;margin-bottom:10px;border-radius:8px;display:flex;justify-content:space-between;align-items:center;}" +
                ".track-title{font-size:18px;font-weight:bold;color:white;}" +
                ".track-meta{font-size:14px;color:#b3b3b3;}" +
                ".fav-btn{cursor:pointer;font-size:24px;color:#555;background:none;border:none;outline:none;}" +
                ".is-fav{color:#1DB954;}" +
                ".del-btn{cursor:pointer;background:none;border:none;color:#555;font-size:18px;margin-left:15px;}" +
                "</style></head><body>" +
                "<div class='sidebar'>" +
                "<h2 style='color:#1DB954;'>🎧 My Library</h2>" +
                "<button class='nav-btn' onclick='showPage(\"home\")'>🏠 All Tracks</button>" +
                "<button class='nav-btn' onclick='showPage(\"favPage\")'>🌟 Favorites</button>" +
                "<button class='nav-btn' onclick='showPage(\"addSongPage\")'>➕ Add Song</button>" +
                "<button class='nav-btn' onclick='showPage(\"addPodPage\")'>🎙️ Add Podcast</button>" +
                "<input id='searchBar' placeholder='🔍 Search library...' onkeyup='filterTracks()' style='margin-top:20px;width:90%;font-size:14px;'>" +
                "</div>" +
                "<div class='content'>" +
                "  <div id='home' class='page active-page'><h1>📑 All Tracks</h1><div id='listAll'></div></div>" +
                "  <div id='favPage' class='page'><h1>💖 Your Favorites</h1><div id='listFavs'></div></div>" +
                "  <div id='addSongPage' class='page'><h1>🎵 Add New Song</h1><input id='st' placeholder='Song Title'><input id='sa' placeholder='Artist'><input id='sl' placeholder='Language'><input id='sg' placeholder='Genre'><button class='action' onclick='addS()'>✨ Save Song</button></div>" +
                "  <div id='addPodPage' class='page'><h1>🎙️ Add New Podcast</h1><input id='pt' placeholder='Podcast Title'><input id='ph' placeholder='Host'><input id='pd' placeholder='Duration (secs)'><input id='pe' placeholder='Episode #'><button class='action' onclick='addP()'>✨ Save Podcast</button></div>" +
                "</div>" +
                "<script>" +
                "let allData = [];" +
                "function showPage(id){" +
                "  document.querySelectorAll('.page').forEach(p=>p.classList.remove('active-page'));" +
                "  document.getElementById(id).classList.add('active-page');" +
                "  load();" +
                "}" +
                "function filterTracks(){" +
                "  const term = document.getElementById('searchBar').value.toLowerCase();" +
                "  render(allData.filter(t => t.t.toLowerCase().includes(term) || t.a.toLowerCase().includes(term)));" +
                "}" +
                "function load(){ fetch('/api/tracks').then(r=>r.json()).then(data=>{ allData = data; filterTracks(); }); }" +
                "function render(data){" +
                "  const listAll=document.getElementById('listAll'); const listFavs=document.getElementById('listFavs');" +
                "  listAll.innerHTML=''; listFavs.innerHTML='';" +
                "  data.forEach(tr=>{" +
                "    let card = `<div class='track-card'><div><div class='track-title'>${tr.t}</div>` +" +
                "               `<div class='track-meta'>${tr.type} • 👤 ${tr.a} • ${tr.info}</div></div>` +" +
                "               `<div><button class='fav-btn ${tr.f?\"is-fav\":\"\"}' onclick='fav(\"${tr.id}\")'>${tr.f?\"🌟\":\"⭐\"}</button>` +" +
                "               `<button class='del-btn' onclick='del(\"${tr.id}\")'>🗑️</button></div></div>`;" +
                "    listAll.innerHTML += card; if(tr.f) listFavs.innerHTML += card;" +
                "  });" +
                "}" +
                "function addS(){ let p=new URLSearchParams();p.append('t',document.getElementById('st').value);p.append('a',document.getElementById('sa').value);p.append('l',document.getElementById('sl').value);p.append('g',document.getElementById('sg').value); fetch('/api/addS',{method:'POST',body:p}).then(()=>{showPage('home');}); }" +
                "function addP(){ let p=new URLSearchParams();p.append('t',document.getElementById('pt').value);p.append('h',document.getElementById('ph').value);p.append('d',document.getElementById('pd').value);p.append('e',document.getElementById('pe').value); fetch('/api/addP',{method:'POST',body:p}).then(()=>{showPage('home');}); }" +
                "function fav(id){ let p=new URLSearchParams();p.append('id',id); fetch('/api/fav',{method:'POST',body:p}).then(()=>load()); }" +
                "function del(id){ if(confirm('Remove?')){ let p=new URLSearchParams();p.append('id',id); fetch('/api/del',{method:'POST',body:p}).then(()=>load()); } }" +
                "load();" +
                "</script></body></html>";
            
            byte[] b = html.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, b.length);
            t.getResponseBody().write(b);
            t.getResponseBody().close();
        });

        server.createContext("/api/tracks", (t) -> {
            StringBuilder sb = new StringBuilder("[");
            for(int i=0; i<playlist.size(); i++){
                AudioTrack tr = playlist.get(i);
                sb.append("{\"id\":\"").append(tr.getId()).append("\",\"t\":\"").append(tr.getTitle())
                  .append("\",\"a\":\"").append(tr.getArtist()).append("\",\"type\":\"").append(tr.getType())
                  .append("\",\"info\":\"").append(tr.getSubInfo()).append("\",\"f\":").append(tr.isFavorite()).append("}");
                if(i < playlist.size()-1) sb.append(",");
            }
            sb.append("]");
            byte[] b = sb.toString().getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            t.sendResponseHeaders(200, b.length);
            t.getResponseBody().write(b);
            t.getResponseBody().close();
        });

        server.createContext("/api/addS", (t) -> handlePost(t, "S"));
        server.createContext("/api/addP", (t) -> handlePost(t, "P"));
        server.createContext("/api/fav", (t) -> handlePost(t, "F"));
        server.createContext("/api/del", (t) -> handlePost(t, "D"));
        server.start();
        System.out.println("Server started at: http://localhost:8085");
    }

    private static void handlePost(HttpExchange t, String type) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(t.getRequestBody(), StandardCharsets.UTF_8));
        Map<String, String> params = new HashMap<>();
        String line = br.readLine();
        if(line != null) {
            for(String pair : line.split("&")) {
                String[] kv = pair.split("=");
                if(kv.length > 0) {
                    params.put(URLDecoder.decode(kv[0], "UTF-8"), kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "");
                }
            }
        }
        try {
            if(type.equals("S")) playlist.add(new Song(params.get("t"), params.get("a"), params.get("l"), params.get("g")));
            else if(type.equals("P")) playlist.add(new Podcast(params.get("t"), params.get("h"), Integer.parseInt(params.get("d")), Integer.parseInt(params.get("e"))));
            else if(type.equals("F")) { String id = params.get("id"); for(AudioTrack tr : playlist) if(tr.getId().equals(id)) tr.setFavorite(!tr.isFavorite()); }
            else if(type.equals("D")) { String id = params.get("id"); playlist.removeIf(tr -> tr.getId().equals(id)); }
        } catch(Exception e) {}
        save();
        t.sendResponseHeaders(200, -1);
        t.getResponseBody().close();
    }

    private static void save() { 
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE))) { 
            oos.writeObject(playlist); 
        } catch (Exception e) {} 
    }

    @SuppressWarnings("unchecked")
    private static void load() { 
        File f = new File(FILE); 
        if(f.exists()){ 
            try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))){ 
                playlist = (List<AudioTrack>) ois.readObject(); 
            } catch(Exception e) {}
        } 
    }
}