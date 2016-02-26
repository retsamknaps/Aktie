package aktie.gui;

import org.eclipse.jface.viewers.ICellEditorValidator;

public class FieldDecValidator implements ICellEditorValidator
{

    public double min;
    public double max;
    @Override
    public String isValid ( Object arg0 )
    {
        String r = null;
        String ts = ( String ) arg0;

        try
        {
            double v = Double.valueOf ( ts );

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
