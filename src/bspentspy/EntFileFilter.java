package bspentspy;

import java.io.File;

import javax.swing.filechooser.FileFilter;

class EntFileFilter
extends FileFilter {
    EntFileFilter() {
    }

    public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = this.getExtension(f);
            if (extension != null && extension.equals("bsp")) {
                return true;
            }
        }
        return false;
    }

    public String getDescription() {
        return "Source map file (*.bsp)";
    }

    public String getExtension(File f) {
        int i;
        String filename;
        if (f != null && (i = (filename = f.getName()).lastIndexOf(46)) > 0 && i < filename.length() - 1) {
            return filename.substring(i + 1).toLowerCase();
        }
        return null;
    }
}

