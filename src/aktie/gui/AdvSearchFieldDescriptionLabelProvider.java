package aktie.gui;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import aktie.data.CObj;

public class AdvSearchFieldDescriptionLabelProvider extends  StyledCellLabelProvider
{

    public AdvSearchFieldDescriptionLabelProvider ( )
    {
    }

    @Override
    public void update ( ViewerCell cell )
    {
        CObjListGetter o = ( CObjListGetter ) cell.getElement();
        CObj co = o.getCObj();

        if ( co != null )
        {
            StringBuilder sb = new StringBuilder();

            String t = co.getString ( CObj.FLD_TYPE );

            if ( t != null )
            {
                sb.append ( t );
                sb.append ( ": " );
            }

            String d = co.getString ( CObj.FLD_DESC );

            if ( d != null )
            {
                sb.append ( d );
                sb.append ( ": " );
            }

            if ( CObj.FLD_TYPE_DECIMAL.equals ( t ) )
            {
                Double min = co.getDecimal ( CObj.FLD_MIN );

                if ( min != null )
                {
                    sb.append ( "Min: " );
                    sb.append ( min.toString() );
                }

                Double max = co.getDecimal ( CObj.FLD_MAX );

                if ( max != null )
                {
                    sb.append ( ", Max: " );
                    sb.append ( max.toString() );
                }

            }

            if ( CObj.FLD_TYPE_NUMBER.equals ( t ) )
            {
                Long min = co.getNumber ( CObj.FLD_MIN );

                if ( min != null )
                {
                    sb.append ( "Min: " );
                    sb.append ( min.toString() );
                }

                Long max = co.getNumber ( CObj.FLD_MAX );

                if ( max != null )
                {
                    sb.append ( ", Max: " );
                    sb.append ( max.toString() );
                }

            }

            String r = sb.toString();

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
