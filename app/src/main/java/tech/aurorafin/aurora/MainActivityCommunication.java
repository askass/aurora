package tech.aurorafin.aurora;

import java.util.HashMap;

public interface MainActivityCommunication {
    void setAppbarText(String s);
    void makeToast(final int toast);
    int getBottomNavigationHeight();
    void hideBottomNavigation();
    void showBottomNavigation();
    void transitToAnalysis(int dateCodeFrom, int dateCodeTo, boolean rev, boolean exp, boolean cap, boolean factChip, boolean selectAll);
    void transitionToOperations(long categoryId, int year);
    void setAnalysisSelectedCategories(HashMap<Long, Boolean> appliedSelectedCategories);
    void askForFeedback();
}
