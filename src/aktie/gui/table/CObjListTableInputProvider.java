package aktie.gui.table;

import org.apache.lucene.search.Sort;

import aktie.index.CObjList;

public class CObjListTableInputProvider
{

    private Sort sort = null;
    private CObjList currentInput = null;

    public Sort getSort()
    {
        return sort;
    }

    /**
        To be overriden.
        @param sort The Sort put in here by CObjTableViewer
        @return A CObjList representing the input which is sorted according sort.
    */
    public CObjList provideInput ( Sort sort )
    {
        return null;
    }

    /**
        Calls provideInput which should have been overriden by an implementing superclass.
        Puts sort into provideInput and closes the CObjList which is the current input.
        @param sort the Sort put into provideInput
        @return The CObjList returned by provideInput or the CObjList currently set as input if provideInput returns null. May be null.
    */
    public CObjList getInput ( Sort sort )
    {
        //System.out.println( "CObjListTableInputProvider.getInput()" );
        this.sort = sort;
        CObjList newInput = provideInput ( sort );

        if ( newInput != null )
        {
            if ( currentInput != null )
            {
                currentInput.close();
            }

            return newInput;
        }

        return currentInput;
    }

}
