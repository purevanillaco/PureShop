package ovh.quiquelhappy.mcplugins.pureshop;

import com.connorlinfoot.titleapi.TitleAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class main extends JavaPlugin {

    public static Plugin plugin = null;
    public static String key = null;

    public void onEnable(){
        plugin=this;

        createHeader("");

        System.out.println("  _____                 _____ _                 ");
        System.out.println(" |  __ \\               / ____| |                ");
        System.out.println(" | |__) |   _ _ __ ___| (___ | |__   ___  _ __  ");
        System.out.println(" |  ___/ | | | '__/ _ \\\\___ \\| '_ \\ / _ \\| '_ \\ ");
        System.out.println(" | |   | |_| | | |  __/____) | | | | (_) | |_) |");
        System.out.println(" |_|    \\__,_|_|  \\___|_____/|_| |_|\\___/| .__/ ");
        System.out.println("                                         | |    ");
        System.out.println("                                         |_|    ");

        createHeader("CONFIG");

        if ((new File("plugins" + File.separator + "PureShop" + File.separator + "config.yml")).isFile()) {
            System.out.println("[PureShop] Loading config");
        } else {
            System.out.println("[PureShop] Creating config");
            this.saveDefaultConfig();
            this.getConfig().options().copyDefaults(true);
        }

        FileConfiguration config = this.getConfig();
        key = config.getString("key");

        System.out.println("[PureShop] Starting checker loop");

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                checkNew();
            }
        }, 0, 20000);

    }

    private boolean createHeader(String header){
        System.out.println(" ");
        System.out.println(" ");
        System.out.println(header);
        System.out.println(" ");
        return true;
    }

    @Override
    public void onDisable(){
        super.onDisable();
        createHeader("DISABLING PLUGIN");
    }

    public void checkNew(){
        URL url;
        try {
            url = new URL("https://www.purevanilla.es/api/store/pending/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            con.disconnect();

            String response = content.toString();
            JsonArray paymentsArray = new JsonParser().parse(response).getAsJsonArray();

            for (JsonElement pa : paymentsArray) {
                JsonObject paymentObj = pa.getAsJsonObject();
                String     username     = paymentObj.get("username").getAsString();
                String     product      = paymentObj.get("package").getAsString();
                Integer    id           = paymentObj.get("id").getAsInt();

                validatePurchase(username,product,id);

            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void validatePurchase(String username, String product, Integer id){
        System.out.println(username+" purchased "+product+" (#"+id.toString()+")");

        URL url;
        try {
            url = new URL("https://www.purevanilla.es/api/store/validate?id="+id+"&key="+key);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            con.disconnect();

            String response = content.toString();
            JsonObject result = new JsonParser().parse(response).getAsJsonObject();

            if(result.get("done").getAsBoolean()){

                for(Player all:getServer().getOnlinePlayers()){
                    final Player player = all.getPlayer();
                    TitleAPI.sendTitle(player,10,800,10,"&7&l"+username,"just bought &e&l"+product);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0);

                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    TitleAPI.sendTitle(player,10,200,10,"","&7&lGet your rank here");
                                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 0);
                                }
                            },
                            10000
                    );

                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    TitleAPI.sendTitle(player,10,60,10,"","&ehttps://purevanilla.es/store");
                                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0);
                                }
                            },
                            14000
                    );

                }

                if(product.equals("hero")){
                    System.out.println("validated #"+id+". Rank: hero");
                    Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand( Bukkit.getConsoleSender(), "adjustbonusclaimblocks "+username+" 2500" ) ).get();
                    Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand( Bukkit.getConsoleSender(), "lp user "+username+" parent set hero" ) ).get();

                } else if(product.equals("elite")){
                    System.out.println("validated #"+id+". Rank: elite");
                    Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand( Bukkit.getConsoleSender(), "adjustbonusclaimblocks "+username+" 5000" ) ).get();
                    Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand( Bukkit.getConsoleSender(), "lp user "+username+" parent set elite" ) ).get();

                } else if(product.equals("platinum")){
                    System.out.println("validated #"+id+". Rank: platinum");
                    Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand( Bukkit.getConsoleSender(), "adjustbonusclaimblocks "+username+" 10000" ) ).get();
                    Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand( Bukkit.getConsoleSender(), "lp user "+username+" parent set platinum" ) ).get();

                } else if(product.equals("immortal")){
                    System.out.println("validated #"+id+". Rank: immortal");
                    Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand( Bukkit.getConsoleSender(), "adjustbonusclaimblocks "+username+" 25000") ).get();
                    Bukkit.getScheduler().callSyncMethod( this, () -> Bukkit.dispatchCommand( Bukkit.getConsoleSender(), "lp user "+username+" parent set immortal") ).get();

                } else {
                    System.out.println("validated #"+id+". Unknown rank: "+product);

                }
            } else {
                System.out.println("couldn't validate #"+id+". Error: "+result.get("error").getAsString());
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
