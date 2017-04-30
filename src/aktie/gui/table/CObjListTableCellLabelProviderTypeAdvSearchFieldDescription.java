package aktie.gui.table;

import org.apache.lucene.search.SortField;

import aktie.data.CObj;

public class CObjListTableCellLabelProviderTypeAdvSearchFieldDescription extends CObjListTableCellLabelProvider
{

    public CObjListTableCellLabelProviderTypeAdvSearchFieldDescription()
    {
        super ( "", false, SortField.Type.STRING, "" );
    }

    @Override
    public String getFormattedAttribute ( CObj co )
    {
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

            return sb.toString();
        }

        return "";
    }

}
