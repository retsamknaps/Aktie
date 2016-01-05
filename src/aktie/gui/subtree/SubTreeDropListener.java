package aktie.gui.subtree;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;

public class SubTreeDropListener extends ViewerDropAdapter
{

    private SubTreeModel model;

    public SubTreeDropListener ( Viewer viewer, SubTreeModel mod )
    {
        super ( viewer );
        model = mod;
    }

    @Override
    public boolean performDrop ( Object d )
    {
        Object tar = this.getCurrentTarget();

        if ( tar != null && tar instanceof SubTreeEntity )
        {
            model.dropped ( d, tar, getCurrentLocation() );
            getViewer().setInput ( "Here's some dropped data man" );
            Viewer vr = getViewer();

            if ( vr instanceof TreeViewer )
            {
                TreeViewer tv = ( TreeViewer ) vr;
                model.setCollaspseState ( tv );
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean validateDrop ( Object arg0, int arg1, TransferData arg2 )
    {
        return true;
    }

}
