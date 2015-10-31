package aktie.gui;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import aktie.data.CObj;

public class CObjListStringColumnLabelProvider extends  StyledCellLabelProvider
{
    private String key;

    public CObjListStringColumnLabelProvider ( String k )
    {
        key = k;
    }

    @Override
    public void update ( ViewerCell cell )
    {
        CObjListGetter o = ( CObjListGetter ) cell.getElement();
        CObj co = o.getCObj();

        if ( co != null )
        {
            String r = co.getString ( key );

            if ( r == null )
            {
                r = "";
            }

            cell.setText ( r );

            Long nv = co.getPrivateNumber ( CObj.PRV_TEMP_NEWPOSTS );

            if ( nv != null && 1L == nv )
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


}
