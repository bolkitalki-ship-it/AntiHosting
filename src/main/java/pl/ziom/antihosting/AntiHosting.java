package pl.ziom.antihosting;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class AntiHosting extends JavaPlugin {

    private ProtocolManager protocolManager;
    private List<String> blockedKeywords;
    private boolean logBlocked;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("==================================================");
            getLogger().severe("BŁĄD: Nie znaleziono wymaganego pluginu ProtocolLib!");
            getLogger().severe("AntiHosting wymaga ProtocolLib do blokowania reklam.");
            getLogger().severe("Plugin zostanie wyłączony.");
            getLogger().severe("==================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        protocolManager = ProtocolLibrary.getProtocolManager();
        registerPacketListeners();

        getLogger().info("AntiHosting (Paper 1.21.4) został pomyślnie uruchomiony!");
    }

    @Override
    public void onDisable() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(this);
        }
    }

    public void loadConfigValues() {
        reloadConfig();
        blockedKeywords = getConfig().getStringList("blocked-keywords");
        logBlocked = getConfig().getBoolean("log-blocked-messages", true);
    }

    private void registerPacketListeners() {
        protocolManager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.SYSTEM_CHAT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    Component message = event.getPacket().getChatComponents().read(0);
                    if (message != null) {
                        String plainText = PlainTextComponentSerializer.plainText().serialize(message);
                        if (containsBlockedKeyword(plainText)) {
                            event.setCancelled(true);
                            if (logBlocked) {
                                getLogger().info("Zablokowano reklamę hostingu: " + plainText);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    private boolean containsBlockedKeyword(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String keyword : blockedKeywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
