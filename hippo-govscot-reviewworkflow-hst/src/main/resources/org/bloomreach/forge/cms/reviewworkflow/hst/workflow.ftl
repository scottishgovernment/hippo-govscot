<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"] >
<#assign fmt=JspTaglibs ["http://java.sun.com/jsp/jstl/fmt"] >

<@hst.defineObjects />

<#if isPreview && eligibleForReview>

    <@hst.actionURL var="actionLink"/>
    <form action="${actionLink}" method="post">
        <!-- form fields here -->
        <input type="radio" name="workflow" value="accept"/>Accept<br>
        <input type="radio" name="workflow" value="reject"/>Reject<br>
        Reason:
        <input type="text" name="reason"/><br>
        <input type="submit" value="Submit"/>
    </form>

</#if>
