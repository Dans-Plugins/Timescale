package com.dansplugins.timescale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles {@code /timescale}, which shows the day and night ratios, and
 * {@code /timescale set <day> <night>}, which changes and saves them.
 *
 * <p>The reply is worked out by {@link #execute} without touching Bukkit, so it can be unit-tested
 * without a server.
 */
public final class TimescaleCommand implements CommandExecutor {

    public static final String SET_PERMISSION = "timescale.set";
    public static final String USAGE = "Usage: /timescale [set <day> <night>]";
    public static final String SET_USAGE = "Usage: /timescale set <day> <night>";

    /** Where the ratios are read from and saved to. */
    public interface Ratios {
        double getDayRatio();

        double getNightRatio();

        /** Applies and persists both ratios, which {@link #execute} has already validated. */
        void setRatios(double dayRatio, double nightRatio);
    }

    private final Ratios ratios;

    public TimescaleCommand(Ratios ratios) {
        this.ratios = ratios;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(execute(args, sender.hasPermission(SET_PERMISSION), ratios));
        return true;
    }

    /** Runs the command and returns the message to send back. */
    static String execute(String[] args, boolean canSet, Ratios ratios) {
        if (args.length == 0) {
            return "Day ratio " + ratios.getDayRatio() + ", night ratio " + ratios.getNightRatio()
                    + " (1.0 is vanilla speed)";
        }
        if (!args[0].equalsIgnoreCase("set")) {
            return USAGE;
        }
        if (!canSet) {
            return "You need the " + SET_PERMISSION + " permission to change the ratios.";
        }
        if (args.length != 3) {
            return SET_USAGE;
        }
        Double dayRatio = parseRatio(args[1]);
        if (dayRatio == null) {
            return "Day ratio must be a positive number but was '" + args[1] + "'. " + SET_USAGE;
        }
        Double nightRatio = parseRatio(args[2]);
        if (nightRatio == null) {
            return "Night ratio must be a positive number but was '" + args[2] + "'. " + SET_USAGE;
        }
        ratios.setRatios(dayRatio, nightRatio);
        return "Day ratio set to " + dayRatio + ", night ratio set to " + nightRatio + ".";
    }

    /** Returns the ratio, or {@code null} if it is not a positive, finite number. */
    private static Double parseRatio(String text) {
        double ratio;
        try {
            ratio = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
        if (!Double.isFinite(ratio) || ratio <= 0.0) {
            return null;
        }
        return ratio;
    }
}
