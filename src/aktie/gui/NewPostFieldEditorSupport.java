package aktie.gui;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;

public class NewPostFieldEditorSupport extends EditingSupport
{

    public NewPostFieldEditorSupport ( ColumnViewer viewer )
    {
        super ( viewer );
    }

    @Override
    protected boolean canEdit ( Object arg0 )
    {
        return false;
    }

    @Override
    protected CellEditor getCellEditor ( Object arg0 )
    {
        return null;
    }

    @Override
    protected Object getValue ( Object arg0 )
    {
        return null;
    }

    @Override
    protected void setValue ( Object arg0, Object arg1 )
    {

    }

}
