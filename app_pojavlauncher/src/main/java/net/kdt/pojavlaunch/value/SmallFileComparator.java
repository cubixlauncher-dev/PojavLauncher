package net.kdt.pojavlaunch.value;

import java.util.Comparator;

public class SmallFileComparator implements Comparator<CubixFileInfo> {
    @Override
    public int compare(CubixFileInfo o1, CubixFileInfo o2) {
        return (int) (o1.size - o2.size);
    }
}
