import java.util.*;

public class Graph{
    ArrayList<LinkedList<Page>> alist;

    Graph(){
        alist = new ArrayList<>();
    }

    public void addPage(Page page) {
        LinkedList<Page> currentList = new LinkedList<>();
        currentList.add(page);
        alist.add(currentList);
    }
    public void addEdge(int src, int dst) {
        LinkedList<Page> currentList = alist.get(src);
        Page dstPage = alist.get(dst).get(0);
        currentList.add(dstPage);
    }
    public boolean checkEdge(int src, int dst) {
        LinkedList<Page> currentList = alist.get(src);
        Page dstPage = alist.get(dst).get(0);

        for(Page page : currentList) {
            if(page == dstPage) {
                return true;
            }
        }
        return false;
    }

    public void print(){
        for(LinkedList<Page> currentList : alist) {
            int size = currentList.size();
            int currentIndex = 0;
            for(Page page : currentList) {
                System.out.print(page.pageName);
                //this is so it doesnt look ugly
                if (++currentIndex < size){
                    System.out.print(" -> ");
                }
            }
            System.out.println();
        }
    }
    public int getTotalEdges(){
        int totalEdges = 0;
        for (LinkedList<Page> currentList : alist) {
            totalEdges += currentList.size()-1; //excluding the page itself
        }
        return totalEdges;
    }
}


