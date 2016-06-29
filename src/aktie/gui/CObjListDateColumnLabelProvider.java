package aktie.gui;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import aktie.data.CObj;

public class CObjListDateColumnLabelProvider extends StyledCellLabelProvider
{

    private String key;
    private SimpleDateFormat dateformat;
    private boolean prvDate;


    public CObjListDateColumnLabelProvider ( String k )
    {
        this ( k, false );
    }

    public CObjListDateColumnLabelProvider ( String k, boolean prv )
    {
        key = k;
        prvDate = prv;
        dateformat = new SimpleDateFormat ( "d MMM yyyy HH:mm z" );
    }

    @Override
    public void update ( ViewerCell cell )
    {
        CObjListGetter o = ( CObjListGetter ) cell.getElement();
        CObj co = o.getCObj();

        if ( co != null )
        {
            String r = "";

            Long cl = null;

            if ( !prvDate )
            {
                cl = co.getNumber ( key );
            }

            else
            {
                cl = co.getPrivateNumber ( key );
            }

            if ( cl != null )
            {
                r = dateformat.format ( new Date ( cl ) );
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

    }

}
