<li class="ds_search-result">
    <h3 class="ds_search-result__title">
        <a class="ds_search-result__link" href="${result.link.url}">${result.link.label}</a>
    </h3>
    <#if result.image!?has_content>
        <#assign path = result.image />
        <#assign norm = path?trim?replace("/+$", "", "r") />
        <#assign lastElement = norm?split("/")?last />

        <div class="ds_search-result__has-media">
            <div class="ds_search-result__media-wrapper">
                <a class="ds_search-result__media-link" aria-hidden="true" href="${result.link.url}" tabindex="-1">

                    <div class="ds_search-result__media  ds_aspect-box  ds_aspect-box--square">
                       <img
                         class="ds_aspect-box__inner"
                         alt=""
                         width="96" height="96" loading="lazy"
                         srcset="<#list result.image.sizes as img>${img}<#if img?has_next>, </#if></#list>"
                         sizes="(min-width:480px) 128px, 96px"
                         src="${result.image.image}"
                          />

                    </div>
                </a>
            </div>
        <div>


    </#if>

    <#if result.subtitle?has_content>
        <h4 class="ds_search-result__sub-title">${result.subtitle}</h4>
    </#if>

    <p class="ds_search-result__summary">
        <#if result.summary??>
            <@highlightSearchTerm result.summary />
        </#if>
    </p>

    <#if (displayLabels && result.label?has_content) || (displayDates && result.displayDate??)>
        <dl class="ds_search-result__metadata  ds_metadata  ds_metadata--inline">
            <#if result.label?has_content && displayLabels>
                <div class="ds_metadata__item">
                    <dt class="ds_metadata__key">Format</dt>
                    <dd class="ds_metadata__value">${result.label}</dd>
                </div>
            </#if>

            <#if displayDates && result.displayDate??>
                <div class="ds_metadata__item">
                    <dt class="ds_metadata__key">Date</dt>
                    <dd class="ds_metadata__value">${result.displayDate}</dd>
                </div>
            </#if>
        </dl>
    </#if>

    <#if result.partOf?has_content>
        <dl class="ds_search-result__context">
            <dt class="ds_search-result__context-key">Part of:</dt>
                <#list result.partOf as partOf>
                    <dd class="ds_search-result__context-value">
                        <a href="${partOf.url}">${partOf.label}</a>
                    </dd>
                </#list>
            </dt>
        </dl>
    </#if>

</li>