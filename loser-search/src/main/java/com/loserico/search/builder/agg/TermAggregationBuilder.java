package com.loserico.search.builder.agg;

/**
 * <p>
 * Copyright: (C), 2021-06-18 21:33
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public interface TermAggregationBuilder extends ElasticAggregationBuilder, Compositable{
	
	public ElasticTermsAggregationBuilder size(Integer size);
}