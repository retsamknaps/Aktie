package aktie.gui;

import org.eclipse.jface.viewers.ICellEditorValidator;

public class FieldNumValidator implements ICellEditorValidator
{

    public long min;
    public long max;
    @Override
    public String isValid ( Object arg0 )
    {
        String r = null;
        String ts = ( String ) arg0;

        try
        {
            long v = Long.valueOf ( ts );

            if ( v < min )
            {
                r = "The minimum value is " + min;
            }

            if ( v > max )
            {
                r = "The maximum value is " + max;
            }

        }

        catch ( Exception e )
        {
            r = "Must be a whole number.";
        }

        return r;
    }


}
