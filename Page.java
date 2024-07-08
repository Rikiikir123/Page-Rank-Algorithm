import java.util.ArrayList;
import java.util.List;

class Page {
    public double rank = 0.0;
    public char pageName;
    public List<Page> connectsToPages = new ArrayList<>();
    public List<Page> isConnectedByPages = new ArrayList<>();
    public int numConnectsToIt = 0;

    //constr
    Page(char pageName) {
        Main.pagecount++;
        this.pageName = pageName;
    }
}
