package aktie.gui;

import aktie.crypto.Utils;
import aktie.data.CObj;

import java.util.Formatter;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

public class CObjListHexColumnLabelProvider extends StyledCellLabelProvider
{

    private String key;

    public CObjListHexColumnLabelProvider ( String k )
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

            if ( r != null )
            {
                byte bytes[] = Utils.toByteArray ( r );
                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter ( sb );

                for ( int c = 0; c < bytes.length; c++ )
                {
                    formatter.format ( "%02X", 0xFF & bytes[c] );
                }

                formatter.close();
                cell.setText ( sb.toString() );

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

        else
        {
            cell.setText ( "" );
        }

    }

}
