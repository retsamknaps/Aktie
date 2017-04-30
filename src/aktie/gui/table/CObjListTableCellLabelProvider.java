package aktie.gui.table;

import org.apache.lucene.search.SortField;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import aktie.data.CObj;
import aktie.gui.CObjListGetter;

public abstract class CObjListTableCellLabelProvider extends AktieTableCellLabelProvider<CObjListGetter>
{

    private String key;
    private boolean privateAttribute;
    private String sortString;
    private SortField.Type sortFieldType;
    private String highlightKey;

    public CObjListTableCellLabelProvider ( String key, boolean privateAttribute, SortField.Type sortFieldType, String highlightKey )
    {
        this.key = key;
        this.privateAttribute = privateAttribute;

        if ( privateAttribute )
        {
            if ( sortFieldType.equals ( SortField.Type.LONG ) )
            {
                sortString = CObj.docPrivateNumber ( key );
            }

            else
            {
                sortString = CObj.docPrivate ( key );
            }

        }

        else
        {
            if ( sortFieldType.equals ( SortField.Type.LONG ) )
            {
                sortString = CObj.docNumber ( key );
            }

            else
            {
                sortString = CObj.docString ( key );
            }

        }

        this.sortFieldType = sortFieldType;
    }

    public String getKey()
    {
        return key;
    }

    public boolean isPrivateAttribute()
    {
        return privateAttribute;
    }

    public String getSortString()
    {
        return sortString;
    }

    public SortField.Type getSortFieldType()
    {
        return sortFieldType;
    }

    public abstract String getFormattedAttribute ( CObj co );

    private Long getHighlight ( CObj co )
    {
        if ( co != null )
        {
            return co.getPrivateNumber ( this.highlightKey );
        }

        return 0L;
    }

    @Override
    public void update ( ViewerCell cell )
    {
        CObjListGetter o = ( CObjListGetter ) cell.getElement();
        CObj co = o.getCObj();

        if ( co != null )
        {
            String r = getFormattedAttribute ( co );
            cell.setText ( r );

            Long highlight = getHighlight ( co );

            if ( highlight != null && highlight == 1L )
            {
                cell.setForeground ( Display.getDefault().getSystemColor ( SWT.COLOR_BLUE ) );
            }

            else
            {
                cell.setForeground ( Display.getDefault().getSystemColor ( SWT.COLOR_WIDGET_FOREGROUND ) );
            }

        }

        else
        {
            cell.setText ( "" );
        }

    }

    @Override
    public int compare ( Object e1, Object e2, boolean reverse )
    {
        //System.out.println ( "CObjListTableCellLabelProvider.compare()" );
        return 0;
    }

}
