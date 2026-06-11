<#ftl output_format="HTML">
<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"] >
<#assign fmt=JspTaglibs ["http://java.sun.com/jsp/jstl/fmt"] >
<@hst.defineObjects />
<@hst.webfile var="iconspath" path="/assets/images/icons/icons.stack.svg"/>
<#setting url_escaping_charset='utf-8'>
<#macro highlightSearchTerm text>
    <#if response.queryHighlightRegex??>
        <#assign pattern = "(?i)(" + response.queryHighlightRegex?replace("(?i)","") + ")" />
        ${text?replace(pattern, "<mark>$1</mark>", 'ri')?no_esc!}
    <#else>
        ${text}
    </#if>
</#macro>

<@hst.headContribution category="googleTagManagerDataLayer">

<script src="<@hst.webfile path='assets/scripts/datalayer-search.js'/>" id="gtm-datalayer-search" data-enabled="${enabled?c}" data-type="${searchType}"></script>
</@hst.headContribution>

<#if enabled>
    <#if response??>
        <#if (response.supplementaryQueries)!?size &gt; 0>
            <#list response.supplementaryQueries as qsup>
                <nav class="ds_search-suggestions" aria-label="Alternative search suggestions">
                    <h2 class="visually-hidden">Also showing results for ${qsup.query}</h2>
                    <p><span aria-hidden="true">Also showing results for</span> <a aria-label="Show results only for ${qsup.query}" href="?${qsup.spellSugestionQuery}">${qsup.query}<#if qsup_has_next>, </#if></a><br />
                       <span aria-hidden="true">Show results only for</span> <a aria-label="Show results only for ${question.originalQuery}" href="?${qsup.qsupSuppressedQuery}">${question.originalQuery}</a></p>
                </nav>
            </#list>
        </#if>

        <#if response.resultsSummary.totalMatching &gt; 0 || response.adverts?size &gt; 0 >
            <h2 class="ds_search-results__title">
                <#if response.resultsSummary.totalMatching <= response.resultsSummary.numRanks ||
                response.resultsSummary.currStart <= response.resultsSummary.numRanks >
                ${response.resultsSummary.totalMatching} <#if response.resultsSummary.totalMatching gt 1>results<#else>result</#if><#if question.originalQuery?has_content> for <span class="ds_search-results__title-query">${question.originalQuery}</span></#if>
                    <#if (response.supplementaryQueries)!?size &gt; 0>
                        <#list response.supplementaryQueries as qsup>or <span class="ds_search-results__title-query">${qsup.query}</span></#list>
                    </#if>
                <#else>
                    Showing ${response.resultsSummary.currStart} to ${response.resultsSummary.currEnd}
                    of ${response.resultsSummary.totalMatching} <#if response.resultsSummary.totalMatching gt 1>results<#else>result</#if><#if question.originalQuery?has_content> for <span class="ds_search-results__title-query">${question.originalQuery}</span></#if>
                    <#if (response.supplementaryQueries)!?size &gt; 0>
                        <#list response.supplementaryQueries as qsup>or <span class="ds_search-results__title-query">${qsup.query}</span></#list>
                    </#if>
                </#if>
            </h2>
        </#if>
        <#if pagination.currentPageIndex = 1>
            <#list response.htmlMessages as message>
                <div class="ds_inset-text">
                    <div class="ds_inset-text__text">
                        ${message?no_esc}
                    </div>
                </div>
            </#list>
        </#if>
        <#include "filter-buttons.ftl"/>
        <#if !response.question.query?has_content && showBlankQueryMessage>
            <h2 class="visually-hidden">Search</h2>
            <div id="no-search-results" class="ds_no-search-results">
                <#if document.blankSearchQueryMessageContentBlocks??>
                    <#list document.blankSearchQueryMessageContentBlocks as contentBlock>
                    <@hst.html hippohtml=contentBlock.content/>
                    </#list>
                </#if>
                <#if document.blankSearchQueryMessage??>
                    <@hst.html hippohtml=document.blankSearchQueryMessage/>
                </#if>
            </div>
        <#elseif !response.hasResults>
            <h2 class="visually-hidden">Search s</h2>
            <div id="no-search-results" class="ds_no-search-results">
                <#if document.noResultsMessageContentBlocks??>
                    <#list document.noResultsMessageContentBlocks as contentBlock>
                    <@hst.html hippohtml=contentBlock.content/>
                    </#list>
                </#if>
                <#if document.noResultsMessage??>
                    <@hst.html hippohtml=document.noResultsMessage/>
                </#if>
            </div>
        </#if>

        <#if response.hasResults>
        <ol start="${response.resultsSummary.currStart?c}" id="search-results-list" class="ds_search-results__list" data-total="${response.resultsSummary.totalMatching?c}">
            <#if pagination.currentPageIndex = 1>
                <#list response.adverts as advert>
                <li class="ds_search-result  ds_search-result--promoted">
                    <div class="ds_search-result--promoted-content">
                        <header class="ds_search-result--promoted-title">Recommended</header>
                        <h3 class="ds_search-result__title">
                            <a class="ds_search-result__link" href="${advert.linkUrl}">${advert.titleHtml?no_esc}</a>
                        </h3>

                        <p class="ds_search-result__summary">
                            <@highlightSearchTerm advert.descriptionHtml />
                        </p>
                    </div>
                </li>
                </#list>
            </#if>

            <#list response.results as result>
                <#include "search-result.ftl">
            </#list>
        </ol>
        </#if>

        <#if pagination.pages?has_content>
            <nav id="pagination" class="ds_pagination" aria-label="Search result pages">
                <ul class="ds_pagination__list">
                <#if pagination.previous??>
                    <li class="ds_pagination__item">
                        <a aria-label="Previous page" class="ds_pagination__link  ds_pagination__link--text  ds_pagination__link--icon" href="${pagination.previous.url}">
                            <svg class="ds_icon" aria-hidden="true" role="img">
                                <use href="${iconspath}#chevron_left"></use>
                            </svg>
                            <span class="ds_pagination__link-label">${pagination.previous.label}</span>
                        </a>
                    </li>
                </#if>

                <#if pagination.first??>
                    <li class="ds_pagination__item">
                        <a aria-label="Page ${pagination.first.label}" class="ds_pagination__link" href="${pagination.first.url}">
                            <span class="ds_pagination__link-label">${pagination.first.label}</span>
                        </a>
                    </li>
                    <li class="ds_pagination__item" aria-hidden="true">
                        <span class="ds_pagination__link  ds_pagination__link--ellipsis">&hellip;</span>
                    </li>
                </#if>

                <#list pagination.pages as page>
                    <li class="ds_pagination__item">
                        <#if page.selected>
                            <a aria-label="Page ${page.label}" aria-current="page" class="ds_pagination__link  ds_current" href="${page.url}">
                                <span class="ds_pagination__link-label">${page.label}</span>
                            </a>
                        <#else>
                            <a aria-label="Page ${page.label}" class="ds_pagination__link" href="${page.url}">
                                <span class="ds_pagination__link-label">${page.label}</span>
                            </a>
                        </#if>
                    </li>
                </#list>

                <#if pagination.last??>
                    <li class="ds_pagination__item" aria-hidden="true">
                        <span class="ds_pagination__link  ds_pagination__link--ellipsis">&hellip;</span>
                    </li>
                    <li class="ds_pagination__item">
                        <a aria-label="Last page, page ${pagination.last.label}" class="ds_pagination__link" href="${pagination.last.url}">${pagination.last.label}</a>
                    </li>
                </#if>

                <#if pagination.next??>
                    <li class="ds_pagination__item">
                        <a aria-label="Next page" class="ds_pagination__link  ds_pagination__link--text  ds_pagination__link--icon" href="${pagination.next.url}">
                            <span class="ds_pagination__link-label">${pagination.next.label}</span>
                            <svg class="ds_icon" aria-hidden="true" role="img">
                                <use href="${iconspath}#chevron_right"></use>
                            </svg>
                        </a>
                    </li>
                </#if>
                </ul>
            </nav>
        </#if>

        <#if response.relatedResults!?size &gt; 0>
            <aside class="ds_search-results__related" aria-labelledby="search-results__related-title">
                <h2 class="ds_search-results__related-title" id="search-results__related-title">Related searches</h2>
                <ul class="ds_no-bullets">
                    <#list response.relatedResults as relatedResult>
                        <li>
                            <a href="?${relatedResult.url}">${relatedResult.label}</a>
                        </li>
                    </#list>
                </ul>
            </aside>
        </#if>

    </#if>

<#else>
    <p>This page has been temporarily disabled.</p>
</#if>
