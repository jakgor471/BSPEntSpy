package entspy;

public class Lump
implements Comparable<Lump> {
    static String[] lumpname = new String[]{"Entities", "Planes", "TexData", "Vertices", "Visibility", "Nodes", "TexInfo", "Faces", "Lighting", "Occlusion", "Leaves", "", "Edges", "SurfEdges", "Models", "Worldlights", "LeafFaces", "LeafBrushes", "Brushes", "BrushSides", "Areas", "AreaPortals", "Portals", "Clusters", "PortalVerts", "ClusterPortals", "DispInfo", "OriginalFaces", "", "PhysCollide", "VertNormals", "VertNormalIndices", "DispLightmapAlphas", "DispVerts", "DispLightmapSamplePositions", "GameLump", "LeafWaterData", "Primatives", "PrimVerts", "PrimIndices", "PakFile", "ClipPortalVerts", "Cubemaps", "TexDataStringData", "TexDataStringTable", "Overlays", "LeafMinDistToWater", "FaceMacroTextureInfo", "DispTris", "PhysCollideSurface", "", "", "", "LightingHDR", "WorldlightsHDR", "LeaflightHDR1", "LeaflightHDR2", "", "", "", "", "", "", "", ""};
    static int[] lumpsize = new int[]{1, 20, 32, 12, 1, 32, 72, 56, 1, 1, 56, 0, 4, 4, 48, 88, 2, 2, 12, 8, 8, 12, 16, 8, 2, 2, 176, 56, 0, 1, 12, 2, 1, 1, 1, 1, 12, 10, 12, 2, 1, 12, 16, 1, 4, 352, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0};
    int index;
    int ofs;
    int len;
    int vers;
    int fourCC;

    public static String name(int lindex) {
        return lumpname[lindex];
    }

    public static int size(int lindex) {
        if (lumpsize[lindex] == 0) {
            return -1;
        }
        return lumpsize[lindex];
    }

    @Override
    public int compareTo(Lump that) {
        return this.ofs - that.ofs;
    }
}

