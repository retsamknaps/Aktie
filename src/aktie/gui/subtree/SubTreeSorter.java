package aktie.gui.subtree;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class SubTreeSorter extends ViewerSorter
{

    @Override
    public int compare ( Viewer viewer, Object e1, Object e2 )
    {
        if ( e1 != null && e2 != null &&
                e1 instanceof SubTreeEntity && e2 instanceof SubTreeEntity )
        {
            SubTreeEntity s1 = ( SubTreeEntity ) e1;
            SubTreeEntity s2 = ( SubTreeEntity ) e2;
            return s1.compareTo ( s2 );
        }

        return 0;
    }

}

