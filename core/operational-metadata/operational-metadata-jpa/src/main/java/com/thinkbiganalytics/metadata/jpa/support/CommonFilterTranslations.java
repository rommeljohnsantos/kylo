package com.thinkbiganalytics.metadata.jpa.support;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.thinkbiganalytics.metadata.jpa.jobrepo.job.QJpaBatchJobExecution;

import org.apache.commons.collections.MultiMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class to map simple filter strings to more complex filters/joins
 *
 * Created by sr186054 on 12/5/16.
 */
public class CommonFilterTranslations {

    public static final String jobExecutionFeedNameFilterKey = "jobInstance.feed.name";

    static final ImmutableMap<String, String> jobExecutionFilters =
        new ImmutableMap.Builder<String, String>()
            .put("feed", jobExecutionFeedNameFilterKey)
            .put("feedName", jobExecutionFeedNameFilterKey)
            .put("feedname", jobExecutionFeedNameFilterKey)
            .put("jobName", "jobInstance.jobName")
            .put("executionId","jobExecutionId")
            .put("executionid", "jobExecutionId")
            .put("job", "jobInstance.jobName").build();


    static final ImmutableMap<Class<? extends EntityPathBase>, Map<String,String>> queryDslFilters = new ImmutableMap.Builder<Class<? extends EntityPathBase>, Map<String,String>>().put(QJpaBatchJobExecution.class,jobExecutionFilters).build();


    /**
     * Check to see if the incoming path,column exist as a filter that should be resolved to a more complex string
     * @param basePath
     * @param column
     * @return
     */
    public static boolean containsFilterMappings(EntityPathBase basePath, String column){
        return containsFilterMappings(basePath.getClass(),column);
    }

    /**
     * if the supplied column matches a common filter for the incoming {@code basePath} object it will resolve the column to the correct filter
     * otherwise it will return the column
     * @param basePath
     * @param column
     * @return
     */
   public static String resolvedFilter(EntityPathBase basePath, String column){
        if(containsFilterMappings(basePath,column)){
            return queryDslFilters.get(basePath.getClass()).get(column);
        }
        return column;
    }

    /**
     * If the incoming {@code column} matches as a key to a more complex filter this will return the more complex filter/column needed for the Join or Sort.  Otherwise it will return the {@code column} as the filter string
     * @param column
     * @return
     */
    public static String getResolvedJobExecutionFilter(String column){
        return getResolvedFilter(QJpaBatchJobExecution.class,column);
    }



    private static String getResolvedFilter(Class<? extends EntityPathBase> clazz, String column){
        if(containsFilterMappings(clazz,column)){
            return queryDslFilters.get(clazz).get(column);
        }
        return null;
    }


    private static boolean containsFilterMappings(Class<? extends  EntityPathBase> clazz, String column){
        return queryDslFilters.containsKey(clazz) && queryDslFilters.get(clazz).containsKey(column);
    }

    /**
     * Update the Pageable sort filter and resolve any filter strings if needed
     * @param base
     * @param pageable
     * @return
     */
    public static Pageable resolveSortFilters(EntityPathBase base,Pageable pageable){

        if(pageable != null && pageable.getSort() != null) {
            List<Sort.Order> sortList = Lists.newArrayList(pageable.getSort().iterator());
            boolean anyMatch = sortList.stream().anyMatch(order -> containsFilterMappings(base,order.getProperty()));
            //if there is a match reconstruct pageable
            if(anyMatch){
                List<Sort.Order> updatedSortOrder = sortList.stream().map(order -> new Sort.Order(order.getDirection(),resolvedFilter(base,order.getProperty()))).collect(Collectors.toList());
                Sort sort= new Sort(updatedSortOrder);
                Pageable updatedPageable = new PageRequest(pageable.getPageNumber(),pageable.getPageSize(),sort);
                return updatedPageable;
            }
        }
        return pageable;

    }

}