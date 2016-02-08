package aktie.gui;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;

import aktie.data.CObj;

import org.eclipse.jface.viewers.CheckboxCellEditor;
//import org.eclipse.swt.SWT;

//import aktie.data.CObj;

public class NewPostFieldEditorSupport extends EditingSupport
{

    private TextCellEditor text;
    private CheckboxCellEditor checkbox;
    private TableViewer tviewer;

    public NewPostFieldEditorSupport ( ColumnViewer viewer )
    {
        super ( viewer );
        tviewer = ( TableViewer ) viewer;
        text = new TextCellEditor ( tviewer.getTable() );
        checkbox = new CheckboxCellEditor ( tviewer.getTable() );
    }

    @Override
    protected boolean canEdit ( Object c )
    {
        if ( c instanceof CObjListGetter )
        {
            CObjListGetter gt = ( CObjListGetter ) c;
            CObj co = gt.getCObj();
            String fdtyp = co.getString ( CObj.FLD_TYPE );

            if ( CObj.FLD_TYPE_STRING.equals ( fdtyp ) )
            {
                return true;
            }

            if ( CObj.FLD_TYPE_BOOL.equals ( fdtyp ) )
            {
                return true;
            }

        }

        return false;
    }

    @Override
    protected CellEditor getCellEditor ( Object c )
    {
        if ( c instanceof CObjListGetter )
        {
            CObjListGetter gt = ( CObjListGetter ) c;
            CObj co = gt.getCObj();
            String fdtyp = co.getString ( CObj.FLD_TYPE );

            if ( CObj.FLD_TYPE_STRING.equals ( fdtyp ) )
            {
                return text;
            }

            if ( CObj.FLD_TYPE_BOOL.equals ( fdtyp ) )
            {
                return checkbox;
            }

        }

        return null;
    }

    @Override
    protected Object getValue ( Object c )
    {
        if ( c instanceof CObjListGetter )
        {
            CObjListGetter gt = ( CObjListGetter ) c;
            CObj co = gt.getCObj();
            String fdtyp = co.getString ( CObj.FLD_TYPE );

            if ( CObj.FLD_TYPE_STRING.equals ( fdtyp ) )
            {
                String v = co.getPrivate ( CObj.FLD_VAL );

                if ( v == null ) { v = ""; }

                return v;
            }

            if ( CObj.FLD_TYPE_BOOL.equals ( fdtyp ) )
            {
                String v = co.getPrivate ( CObj.FLD_VAL );
                return "true".equals ( v );
            }

        }

        return "";
    }

    @Override
    protected void setValue ( Object c, Object v )
    {
        if ( c instanceof CObjListGetter )
        {
            CObjListGetter gt = ( CObjListGetter ) c;
            CObj co = gt.getCObj();
            co.pushPrivate ( CObj.FLD_VAL, v.toString() );
            tviewer.refresh();
        }

    }

}
