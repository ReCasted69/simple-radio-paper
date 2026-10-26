package com.lanvexstudios.service;

import com.lanvexstudios.config.Config;
import com.lanvexstudios.model.Antenna;
import com.lanvexstudios.model.AntennaScan;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class AntennaScanner {

    private final Config config;

    public AntennaScanner(Config config) {
        this.config = config;
    }

    public AntennaScan scan(Block base) {
        Antenna antenna = new Antenna();
        Block current = base.getRelative(BlockFace.UP);
        int height = 0;

        while (isAntenna(current.getType())) {
            Material type = current.getType();
            if (config.rodBlocks.contains(type)) {
                antenna.setRods(antenna.getRods() + 1);
            } else if (config.copperBlocks.contains(type)) {
                antenna.setCopper(antenna.getCopper() + 1);
            } else if (config.netheriteBlocks.contains(type)) {
                antenna.setNetherite(antenna.getNetherite() + 1);
            }
            height++;
            current = current.getRelative(BlockFace.UP);
        }

        return new AntennaScan(antenna, height);
    }

    public boolean isAntenna(Material type) {
        return config.rodBlocks.contains(type)
                || config.copperBlocks.contains(type)
                || config.netheriteBlocks.contains(type);
    }

    public boolean isBase(Material type) {
        return config.baseBlocks.contains(type);
    }

    public boolean hasNoteBlock(Block base) {
        return findNoteBlock(base) != null;
    }

    public Location findNoteBlock(Block base) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block b = base.getRelative(face);
            if (b.getType() == Material.NOTE_BLOCK) {
                return b.getLocation().add(0.5, 0.5, 0.5);
            }
        }
        return null;
    }
}
