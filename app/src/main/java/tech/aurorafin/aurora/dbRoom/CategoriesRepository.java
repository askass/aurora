package tech.aurorafin.aurora.dbRoom;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.concurrent.ThreadPoolExecutor;

import tech.aurorafin.aurora.R;

public class CategoriesRepository {

    private AggregatorDao aggregatorDao;
    private CategoryDao categoryDao;
    ThreadPoolExecutor mExecutor;
    private Handler handler;
    CategoriesUpdateCallback mCategoriesUpdateCallback;
    BkgCategoriesUpdate bkgCategoriesUpdate;
    UpdateNotifier updateNotifier;


    public String[] types;

    public List<ACategory> categories;
    public ArrayList<Aggregator> aggregators;
    public List<Category> lastUsedCategories;
    public List<Category> sortedCategories;

    HashMap<Long, Boolean> mapIdLocked;
    public HashMap<Long, Category> mapIdCategory;


    public CategoriesRepository(Context context, ThreadPoolExecutor executor, Handler handler){
        types = context.getResources().getStringArray(R.array.category_types);
        CashFlowDB db = CashFlowDB.getInstance(context);
        this.aggregatorDao = db.aggregatorDao();
        this.categoryDao = db.categoryDao();
        this.mExecutor = executor;
        this.handler = handler;
        categories = new ArrayList<>();
        aggregators = new ArrayList<>();
        lastUsedCategories = new ArrayList<>();
        sortedCategories = new ArrayList<>();
        mapIdLocked = new HashMap<>();
        mapIdCategory = new HashMap<>();
        bkgCategoriesUpdate = new BkgCategoriesUpdate();
        updateNotifier = new UpdateNotifier();
        updateACategories();
    }

    /*Categories Locker*/

    public int mapIdLockedSize(){
        return mapIdLocked.size();
    }

    public synchronized void lockCategory(long id){
        mapIdLocked.put(id, true);
        indexOfCategory(id);
        if(mCategoriesUpdateCallback!=null){
            handler.post(new LockerNotifier(id, indexOfCategory(id), true));
        }
    }

    public synchronized void unlockCategory(long id){
        if(isCategoryLocked(id)){
            mapIdLocked.remove(id);
            if(mCategoriesUpdateCallback!=null){
                handler.post(new LockerNotifier(id, indexOfCategory(id), false));
            }
        }
    }

    public boolean isCategoryLocked(long id){
        return mapIdLocked.containsKey(id);
    }

    public void updateLockedCategories(HashMap<Long, Boolean> serviceLockerMap){
        if(serviceLockerMap.size() == 0 && mapIdLocked.size() != 0){
            for (Long key : mapIdLocked.keySet()) {
                unlockCategory(key);
            }
        }else if(serviceLockerMap.size() != 0 && mapIdLocked.size() == 0){
            for (Long key : serviceLockerMap.keySet()) {
                 lockCategory(key);
            }
        }else {
            for (Long key : mapIdLocked.keySet()) {
                if(!serviceLockerMap.containsKey(key)){
                    unlockCategory(key);
                }
            }

            for (Long key : serviceLockerMap.keySet()) {
                if(!mapIdLocked.containsKey(key)){
                    lockCategory(key);
                }
            }
        }
    }


    private class LockerNotifier implements Runnable{
        long id;
        int index;
        boolean locked;
        public LockerNotifier(long id, int index, boolean locked){
            this.id =id;
            this.index = index;
            this.locked = locked;
        }
        @Override
        public void run() {
            mCategoriesUpdateCallback.CategoryUpdated(id, index, locked);
        }
    }

    /*SEARCH FUNCTIONS*/

    public String getCategoryNameById(long id) {
        if(mapIdCategory.containsKey(id)){
            return mapIdCategory.get(id).name;
        }else {
            return null;
        }
    }

    public int getCategoryTypeById(long id){
        if(mapIdCategory.containsKey(id)){
            return mapIdCategory.get(id).type;
        }else {
            return -1;
        }
    }

    public int indexOfAggregator(long id){
        int index = -1;
        if (aggregators != null){
            for(int i = 0; i < aggregators.size(); i++){
                if(aggregators.get(i).id == id){
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public int indexOfCategory(long id){
        int index = -1;
        if (categories != null){
            int i = 0;
            for (ACategory cat: categories){
                if(cat.type != ACategory.AGGREGATOR && cat.type != ACategory.EMPTY_AGGREGATOR){
                    if(cat.id == id){
                        index = i;
                        break;
                    }
                }
                i++;
            }
        }
        return index;
    }


    public long getFirstCategoryId(){
    long id = -1;
        for (ACategory cat: categories){
            if(cat.type != ACategory.AGGREGATOR
                && cat.type != ACategory.EMPTY_AGGREGATOR){
                    id = cat.id;
                    break;
                }
        }
        return id;
    }




    public void setCategoriesUpdateCallback(CategoriesUpdateCallback categoriesUpdateCallback){
        this.mCategoriesUpdateCallback = categoriesUpdateCallback;
    }


    public interface CategoriesUpdateCallback{
        void CategoriesUpdated();
        void CategoryUpdated(long id, int index, boolean locked);
    }

    public void insetCategory(Category category){
        mExecutor.execute(new BkgCategoryInsert(category));
    }

    public void updateCategory(Category category){
        mExecutor.execute(new BkgCategoryUpdate(category));
    }

    public void deleteCategory(long id){
        mExecutor.execute(new BkgCategoryDelete(id));
    }

    public void insetAggregator(Aggregator aggregator){
        mExecutor.execute(new BkgAggregatorInsert(aggregator));
    }

    public void updateAggregator(Aggregator aggregator){
        mExecutor.execute(new BkgAggregatorUpdate(aggregator));
    }

    public void deleteAggregator(long id){
        mExecutor.execute(new BkgAggregatorDelete(id));
    }


    public void updateACategories(){
       mExecutor.execute(bkgCategoriesUpdate);
    }


    private class BkgCategoryDelete implements Runnable{
        long categoryId;
        public BkgCategoryDelete(long categoryId){
            this.categoryId = categoryId;
        }
        @Override
        public void run() {
            categoryDao.deleteCategoryById(categoryId);
            categoriesUpdateSequence();
        }
    }

    private class BkgAggregatorDelete implements Runnable{
        long aggregatorId;
        public BkgAggregatorDelete(long aggregatorId){
            this.aggregatorId = aggregatorId;
        }
        @Override
        public void run() {
            Category[] cat = categoryDao.getAllCategoriesOfAggregator(aggregatorId);
            for (int i = 0; i < cat.length; i++){
                cat[i].aggregatorId = 1L;
                categoryDao.update(cat[i]);
            }
            aggregatorDao.deleteAggregatorById(aggregatorId);
            categoriesUpdateSequence();
        }
    }

    private class BkgAggregatorInsert implements Runnable{
        Aggregator mAggregator;
        public BkgAggregatorInsert(Aggregator aggregator){
            this.mAggregator = aggregator;
        }
        @Override
        public void run() {
            aggregatorDao.insert(mAggregator);
            categoriesUpdateSequence();
        }
    }

    private class BkgAggregatorUpdate implements Runnable{
        Aggregator mAggregator;
        public BkgAggregatorUpdate(Aggregator aggregator){
            this.mAggregator = aggregator;
        }
        @Override
        public void run() {
            aggregatorDao.update(mAggregator);
            categoriesUpdateSequence();
        }
    }

    private class BkgCategoryInsert implements Runnable{
        Category mCategory;
        public BkgCategoryInsert(Category category){
            this.mCategory = category;
        }
        @Override
        public void run() {
            categoryDao.insert(mCategory);
            categoriesUpdateSequence();
        }
    }

    private class BkgCategoryUpdate implements Runnable{
        Category mCategory;
        public BkgCategoryUpdate(Category category){
            this.mCategory = category;
        }
        @Override
        public void run() {
            categoryDao.update(mCategory);
            categoriesUpdateSequence();
        }
    }

    private class BkgCategoriesUpdate implements Runnable{
        @Override
        public void run() {
            categoriesUpdateSequence();
        }
    }

    private void categoriesUpdateSequence(){
        getCategoriesFromDatabase();
        if(mCategoriesUpdateCallback != null){
            handler.post(updateNotifier);
        }
    }

    private void getCategoriesFromDatabase(){
        categories.clear();
        aggregators.clear();
        lastUsedCategories.clear();
        mapIdCategory.clear();
        lastUsedCategories = categoryDao.getLastUsedCategories();
        sortedCategories = categoryDao.getSortedCategories();
        List<Aggregator> allAggregators = aggregatorDao.getAllAggregators();
        int tempAggregator = -1;
        for (int i = 0; i<allAggregators.size(); i++){
            if(allAggregators.get(i).type != ACategory.EMPTY_AGGREGATOR){
                populateCategoriesWithAggregator(allAggregators.get(i));
            }else if(allAggregators.get(i).type == ACategory.EMPTY_AGGREGATOR){
                tempAggregator = i;
            }
        }
        if(tempAggregator != -1){
            populateCategoriesWithAggregator(allAggregators.get(tempAggregator));
        }

    }

    private void populateCategoriesWithAggregator(Aggregator aggregator){

        if(aggregator.type == ACategory.EMPTY_AGGREGATOR){
            aggregators.add(0, aggregator);
        }else {
            aggregators.add(aggregator);
        }

        categories.add(new ACategory(
                aggregator.id,
                aggregator.type,
                "",
                true,
                0,
                aggregator.name,
                aggregator.nick,
                false));

        Category[] cat = categoryDao.getAllCategoriesOfAggregator(aggregator.id);

        for (int i = 0; i < cat.length; i++){
            categories.add(new ACategory(
                    cat[i].id,
                    cat[i].type,
                    types[cat[i].type],
                    false,
                    cat[i].aggregatorId,
                    cat[i].name,
                    cat[i].nick,
                    cat[i].active));

            mapIdCategory.put(cat[i].id, cat[i]);
        }

    }



    private class UpdateNotifier implements Runnable{
        @Override
        public void run() {
            mCategoriesUpdateCallback.CategoriesUpdated();
        }
    }



    public static class ACategory{
        public long id;
        public int type;
        public String textType;
        public boolean isAggregator;
        public long aggregatorId;
        public String name;
        public String nick;

        public boolean active;

        public static final int EMPTY_AGGREGATOR = 0;
        public static final int AGGREGATOR = 1;
        public static final int REVENUE = 2;
        public static final int EXPENSE = 3;
        public static final int CAPITAL = 4;

        public ACategory(long id, int type,String textType, boolean isAggregator,long aggregatorId, String name, String nick, boolean active){
            this.id =id;
            this.type = type;
            this.textType = textType;
            this.isAggregator = isAggregator;
            this.aggregatorId = aggregatorId;
            this.name = name;
            this.nick = nick;
            this.active = active;
        }

        /*public String getType(){
            return types[type];
        }*/
    }



}
