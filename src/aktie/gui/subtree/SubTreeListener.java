package aktie.gui.subtree;

import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;

public class SubTreeListener implements ITreeViewerListener
{

    private SubTreeModel model;

    public SubTreeListener ( SubTreeModel m )
    {
        model = m;
    }

    @Override
    public void treeCollapsed ( TreeExpansionEvent ev )
    {
        Object o = ev.getElement();

        if ( o instanceof SubTreeEntity )
        {
            SubTreeEntity et = ( SubTreeEntity ) o;
            model.setCollapsed ( et, true );
        }

    }

    @Override
    public void treeExpanded ( TreeExpansionEvent ev )
    {
        Object o = ev.getElement();

        System.out.println ( "SET TREE EXPANDED: " + o );

        if ( o instanceof SubTreeEntity )
        {
            SubTreeEntity et = ( SubTreeEntity ) o;
            model.setCollapsed ( et, false );
        }

    }

}
