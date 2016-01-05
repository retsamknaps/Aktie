package aktie.gui.subtree;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;

public class SubTreeDragListener implements DragSourceListener
{

    private TreeViewer viewer;

    public SubTreeDragListener ( TreeViewer v )
    {
        viewer = v;
    }

    @Override
    public void dragStart ( DragSourceEvent event )
    {
    }

    @Override
    public void dragSetData ( DragSourceEvent event )
    {
        IStructuredSelection selection = viewer.getStructuredSelection();
        SubTreeEntity firstElement = ( SubTreeEntity ) selection.getFirstElement();

        if ( TextTransfer.getInstance().isSupportedType ( event.dataType ) )
        {
            event.data = Long.toString ( firstElement.getId() );
        }

    }

    @Override
    public void dragFinished ( DragSourceEvent event )
    {
    }

}
