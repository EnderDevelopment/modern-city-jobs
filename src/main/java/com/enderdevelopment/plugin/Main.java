package com.enderdevelopment.plugin;

import java.util.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<UUID, Shift> shifts = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private WalletService wallet;
    private NamespacedKey menuKey;
    private record Shift(Player player, String job, int minutes, int reward, int required, int progress, BossBar bar) {}

    @Override public void onEnable() {
        saveDefaultConfig(); wallet = new WalletService(this); menuKey = new NamespacedKey(this, "menu");
        Bukkit.getPluginManager().registerEvents(this, this);
        new CitizensHook(this).enable();
        Objects.requireNonNull(getCommand("jobs")).setExecutor(this);
        Objects.requireNonNull(getCommand("jobadmin")).setExecutor(this);
    }
    @Override public void onDisable() { for (UUID id : new ArrayList<>(shifts.keySet())) cancel(id, false); }
    public Component message(String text) {
        String prefix = getConfig().getString("messages.prefix", "<green>City Jobs</green> » ");
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(prefix + text);
    }
    private Component msg(String key) { return message(getConfig().getString("messages." + key, "")); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("jobs")) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
            p.sendMessage(msg("opened")); openJobs(p); return true;
        }
        if (!sender.hasPermission("moderncityjobs.admin")) { sender.sendMessage(Component.text("No permission.", NamedTextColor.RED)); return true; }
        if (args.length < 2) { sender.sendMessage(msg("admin-error")); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        try {
            int n = args.length > 2 ? Integer.parseInt(args[2]) : 0;
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "check" -> sender.sendMessage(Component.text(target.getName() + " wallet: " + wallet.balance(target.getUniqueId()) + " emeralds", NamedTextColor.GREEN));
                case "add" -> wallet.add(target.getUniqueId(), n);
                case "remove" -> { if (!wallet.remove(target.getUniqueId(), n)) { sender.sendMessage(Component.text("Invalid amount or insufficient funds.", NamedTextColor.RED)); return true; } }
                case "set" -> wallet.set(target.getUniqueId(), n);
                default -> { sender.sendMessage(msg("admin-error")); return true; }
            }
            sender.sendMessage(Component.text("Wallet balance: " + wallet.balance(target.getUniqueId()) + " emeralds", NamedTextColor.GREEN));
            Player online = target.getPlayer(); if (online != null) online.sendMessage(msg("admin-success").replaceText(b -> b.matchLiteral("{balance}").replacement(String.valueOf(wallet.balance(target.getUniqueId())))));
        } catch (NumberFormatException ex) { sender.sendMessage(msg("admin-error")); }
        return true;
    }
    private List<JobDefinition> jobs() {
        List<JobDefinition> list = new ArrayList<>(); ConfigurationSection sec = getConfig().getConfigurationSection("jobs");
        if (sec != null) for (String id : sec.getKeys(false)) list.add(JobDefinition.from(sec.getConfigurationSection(id), id));
        return list;
    }
    private void openJobs(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("City Job Center", NamedTextColor.DARK_AQUA));
        for (int i=0;i<jobs().size() && i<21;i++) { JobDefinition j=jobs().get(i); ItemStack item=new ItemStack(j.icon()); ItemMeta m=item.getItemMeta(); m.displayName(Component.text(j.display(), NamedTextColor.GOLD)); m.lore(List.of(Component.text(j.task(), NamedTextColor.GRAY), Component.text("Choose this career", NamedTextColor.GREEN))); item.setItemMeta(m); inv.setItem(3+i+(i/7)*2, tagged(item, "job:"+j.id())); }
        inv.setItem(22, tagged(named(Material.EMERALD, "Wallet • " + wallet.balance(p.getUniqueId()) + " emeralds"), "wallet")); p.openInventory(inv);
    }
    private void openDurations(Player p, String job) {
        Inventory inv=Bukkit.createInventory(null,27,Component.text("Select Shift • " + display(job),NamedTextColor.DARK_AQUA));
        int[] slots={11,13,15}; int i=0; ConfigurationSection sec=getConfig().getConfigurationSection("durations");
        if(sec!=null) for(String duration:sec.getKeys(false)) { int mins=Integer.parseInt(duration); int reward=sec.getInt(duration+".reward"); int required=sec.getInt(duration+".required-actions",mins); ItemStack it=named(Material.CLOCK, mins+" minutes"); ItemMeta m=it.getItemMeta(); m.lore(List.of(Component.text("Reward: "+reward+" emeralds",NamedTextColor.GREEN),Component.text("Tasks: "+required,NamedTextColor.GRAY))); it.setItemMeta(m); inv.setItem(slots[i++%3],tagged(it,"duration:"+job+":"+mins)); }
        inv.setItem(22,tagged(named(Material.EMERALD,"Wallet • "+wallet.balance(p.getUniqueId())+" emeralds"),"wallet")); p.openInventory(inv);
    }
    private String display(String id) { ConfigurationSection s=getConfig().getConfigurationSection("jobs."+id); return s==null?id:s.getString("display",id); }
    private ItemStack named(Material mat,String name) { ItemStack i=new ItemStack(mat); ItemMeta m=i.getItemMeta(); m.displayName(Component.text(name,NamedTextColor.GOLD)); i.setItemMeta(m); return i; }
    private ItemStack tagged(ItemStack item,String value) { ItemMeta m=item.getItemMeta(); m.getPersistentDataContainer().set(menuKey,org.bukkit.persistence.PersistentDataType.STRING,value); item.setItemMeta(m); return item; }
    private void openConfirm(Player p,String job,int minutes) {
        int reward=getConfig().getInt("durations."+minutes+".reward",0);
        Inventory inv=Bukkit.createInventory(null,27,Component.text("Confirm • "+display(job),NamedTextColor.DARK_AQUA));
        ItemStack preview=named(Material.EMERALD,"Reward preview: "+reward+" emeralds"); ItemMeta m=preview.getItemMeta(); m.lore(List.of(Component.text(minutes+" minute shift",NamedTextColor.GRAY),Component.text("Only paid after successful completion",NamedTextColor.GREEN))); preview.setItemMeta(m);
        inv.setItem(11,tagged(named(Material.LIME_CONCRETE,"Start shift"),"start:"+job+":"+minutes)); inv.setItem(15,tagged(named(Material.RED_CONCRETE,"Back to jobs"),"back")); inv.setItem(13,preview); p.openInventory(inv);
    }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p)||e.getCurrentItem()==null) return;
        ItemMeta m=e.getCurrentItem().getItemMeta(); if(m==null)return; String action=m.getPersistentDataContainer().get(menuKey,org.bukkit.persistence.PersistentDataType.STRING); if(action==null)return;
        e.setCancelled(true); String[] a=action.split(":");
        if(action.equals("wallet")) { p.sendMessage(msg("wallet").replaceText(b->b.matchLiteral("{balance}").replacement(String.valueOf(wallet.balance(p.getUniqueId()))))); return; }
        if(action.equals("back")) { openJobs(p); return; }
        if(a[0].equals("job")) openDurations(p,a[1]); else if(a[0].equals("duration")) openConfirm(p,a[1],Integer.parseInt(a[2])); else if(a[0].equals("start")) start(p,a[1],Integer.parseInt(a[2]));
    }
    private void start(Player p,String job,int minutes) {
        if(shifts.containsKey(p.getUniqueId())) { p.sendMessage(msg("already-working")); return; }
        long now=System.currentTimeMillis(), wait=getConfig().getLong("settings.cooldown-seconds",5)*1000L;
        if(now-cooldowns.getOrDefault(p.getUniqueId(),0L)<wait) { p.sendMessage(msg("cooldown")); return; }
        int reward=getConfig().getInt("durations."+minutes+".reward",0), required=getConfig().getInt("durations."+minutes+".required-actions",minutes);
        if(reward<=0||required<=0) return;
        BossBar bar=BossBar.bossBar(Component.text(display(job)+" shift • task 0/"+required),0f,BossBar.Color.BLUE,BossBar.Overlay.PROGRESS); p.showBossBar(bar);
        Shift shift=new Shift(p,job,minutes,reward,required,0,bar); shifts.put(p.getUniqueId(),shift); cooldowns.put(p.getUniqueId(),now); p.closeInventory();
        p.sendMessage(message("Shift started: "+display(job)+" — "+minutes+" minutes. Complete tasks by performing the job activity."));
        Bukkit.getScheduler().runTaskLater(this,()->{ if(shifts.get(p.getUniqueId())==shift) cancel(p.getUniqueId(),true); },minutes*60L*20L);
        p.setMetadata("mcj-next-task",new org.bukkit.metadata.FixedMetadataValue(this,System.currentTimeMillis()+getConfig().getLong("settings.action-cooldown-ticks",10)*50L));
    }
    private void progress(Player p) {
        Shift s=shifts.get(p.getUniqueId()); if(s==null)return;
        long next=p.getMetadata("mcj-next-task").isEmpty()?0:p.getMetadata("mcj-next-task").get(0).asLong(); if(System.currentTimeMillis()<next)return;
        p.setMetadata("mcj-next-task",new org.bukkit.metadata.FixedMetadataValue(this,System.currentTimeMillis()+getConfig().getLong("settings.action-cooldown-ticks",10)*50L));
        int n=s.progress()+1; s.bar().progress(Math.min(1f,(float)n/s.required())); s.bar().name(Component.text(display(s.job())+" shift • task "+n+"/"+s.required()));
        if(n>=s.required()) complete(p.getUniqueId()); else shifts.put(p.getUniqueId(),new Shift(p,s.job(),s.minutes(),s.reward(),s.required(),n,s.bar()));
    }
    @EventHandler public void onInteract(PlayerInteractEvent e) { if(e.getAction().isRightClick()) progress(e.getPlayer()); }
    @EventHandler public void onBreak(org.bukkit.event.block.BlockBreakEvent e) { progress(e.getPlayer()); }
    @EventHandler public void onFish(PlayerFishEvent e) { if(e.getState()==PlayerFishEvent.State.CAUGHT_FISH)progress(e.getPlayer()); }
    private void complete(UUID id) { Shift s=shifts.remove(id); if(s==null)return; s.player().hideBossBar(s.bar()); wallet.add(id,s.reward()); s.player().sendMessage(message(getConfig().getString("messages.completed").replace("{reward}",String.valueOf(s.reward())))); }
    private void cancel(UUID id,boolean notify) { Shift s=shifts.remove(id); if(s==null)return; s.player().hideBossBar(s.bar()); if(notify)s.player().sendMessage(msg("cancelled")); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { cancel(e.getPlayer().getUniqueId(),false); }
    @EventHandler public void onDeath(PlayerDeathEvent e) { cancel(e.getEntity().getUniqueId(),true); }
}
