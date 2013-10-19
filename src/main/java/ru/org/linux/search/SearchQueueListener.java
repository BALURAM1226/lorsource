/*
 * Copyright 1998-2013 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.search;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringEscapeUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.org.linux.comment.Comment;
import ru.org.linux.comment.CommentList;
import ru.org.linux.comment.CommentService;
import ru.org.linux.search.SearchQueueSender.UpdateComments;
import ru.org.linux.search.SearchQueueSender.UpdateMessage;
import ru.org.linux.search.SearchQueueSender.UpdateMonth;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SearchQueueListener {
  private static final Logger logger = LoggerFactory.getLogger(SearchQueueListener.class);
  public static final String MESSAGES_INDEX = "messages";
  public static final String MESSAGES_TYPE = "message";

  @Autowired
  private CommentService commentService;
  
  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private Client client;

  @Autowired
  private TopicDao topicDao;

  public void handleMessage(UpdateMessage msgUpdate) throws MessageNotFoundException, IOException {
    logger.info("Indexing "+msgUpdate.getMsgid());

    reindexMessage(msgUpdate.getMsgid(), msgUpdate.isWithComments());
  }

  private void reindexMessage(int msgid, boolean withComments) throws IOException, MessageNotFoundException {
    Topic msg = topicDao.getById(msgid);

    if (!msg.isDeleted() && !msg.isDraft()) {
      updateMessage(msg);
    } else {
      client
              .prepareDelete(MESSAGES_INDEX, MESSAGES_TYPE, Integer.toString(msg.getId()))
              .execute()
              .actionGet();
      //logger.info("Deleting message "+msgid+" from solr");      
    }

    if (withComments) {
      CommentList commentList = commentService.getCommentList(msg, true);

      if (!msg.isDeleted()) {
        reindexComments(msg, commentList);
      } else {
        ImmutableList<Comment> comments = commentList.getList();

        if (!comments.isEmpty()) {
          BulkRequestBuilder bulkRequest = client.prepareBulk();

          for (Comment comment : comments) {
            bulkRequest.add(client.prepareDelete(MESSAGES_INDEX, MESSAGES_TYPE, Integer.toString(comment.getId())));
          }
        }
      }
    }
  }

  private void executeBulk(BulkRequestBuilder bulkRequest) {
    if (bulkRequest.numberOfActions()>0) {
      BulkResponse bulkResponse = bulkRequest.execute().actionGet();

      if (bulkResponse.hasFailures()) {
        logger.warn("Bulk index failed: "+bulkResponse.buildFailureMessage());
        throw new RuntimeException("Bulk request failed");
      }
    }
  }

  public void handleMessage(UpdateComments msgUpdate) throws MessageNotFoundException, IOException {
    logger.info("Indexing comments "+msgUpdate.getMsgids());

    BulkRequestBuilder bulkRequest = client.prepareBulk();

    for (int msgid : msgUpdate.getMsgids()) {
      if (msgid==0) {
        logger.warn("Skipping MSGID=0!!!");
        continue;
      }

      Comment comment = commentService.getById(msgid);

      if (comment.isDeleted()) {
        logger.info("Deleting comment " + comment.getId() + " from solr");
        bulkRequest.add(client.prepareDelete(MESSAGES_INDEX, MESSAGES_TYPE, Integer.toString(comment.getId())));
      } else {
        // комментарии могут быть из разного топика в функция массового удаления
        // возможно для скорости нужен какой-то кеш топиков, т.к. чаще бывает что все
        // комментарии из одного топика
        Topic topic = topicDao.getById(comment.getTopicId());
        String message = msgbaseDao.getMessageText(comment.getId()).getText();
        bulkRequest.add(processComment(topic, comment, message));
      }
    }

    executeBulk(bulkRequest);
  }

  public void handleMessage(UpdateMonth msgUpdate) throws MessageNotFoundException, IOException {
    int month = msgUpdate.getMonth();
    int year = msgUpdate.getYear();

    logger.info("Indexing month "+ year + '/' + month);
    long startTime = System.nanoTime();

    List<Integer> topicIds = topicDao.getMessageForMonth(year, month);
    for(int topicId : topicIds) {
      reindexMessage(topicId, true);
    }

    long endTime = System.nanoTime();
    logger.info("Reindex month "+year+'/'+month+" done, "+(endTime-startTime)/1000000+" millis");
  }

  private void updateMessage(Topic topic) throws IOException {
    Map<String, Object> doc = new HashMap<>();

    doc.put("id", topic.getId());

    doc.put("section_id", topic.getSectionId());
    doc.put("section", topic.getSectionId());
    doc.put("user_id", topic.getUid());
    doc.put("topic_user_id", topic.getUid());
    doc.put("topic_id", topic.getId());
    doc.put("group_id", topic.getGroupId());

    doc.put("title", StringEscapeUtils.unescapeHtml(topic.getTitle()));
    doc.put("topic_title", topic.getTitle());
    doc.put("message", msgbaseDao.getMessageText(topic.getId()).getText());
    Date postdate = topic.getPostdate();
    doc.put("postdate", new Timestamp(postdate.getTime()));

    doc.put("is_comment", false);

    client
            .prepareIndex(MESSAGES_INDEX, MESSAGES_TYPE, Integer.toString(topic.getId()))
            .setSource(doc)
            .execute()
            .actionGet();
  }

  private void reindexComments(Topic topic, CommentList comments) throws IOException {
    BulkRequestBuilder bulkRequest = client.prepareBulk();

    for (Comment comment : comments.getList()) {
      if (comment.isDeleted()) {
        bulkRequest.add(client.prepareDelete(MESSAGES_INDEX, MESSAGES_TYPE, Integer.toString(comment.getId())));
      }
      String message = msgbaseDao.getMessageText(comment.getId()).getText();
      bulkRequest.add(processComment(topic, comment, message));
    }

    executeBulk(bulkRequest);
  }

  private IndexRequestBuilder processComment(Topic topic, Comment comment, String message) {
    Map<String, Object> doc = new HashMap<>();

    doc.put("section_id", topic.getSectionId());
    doc.put("section", topic.getSectionId());
    doc.put("user_id", comment.getUserid());
    doc.put("topic_user_id", topic.getUid());
    doc.put("topic_id", comment.getTopicId());
    doc.put("group_id", topic.getGroupId());
    String topicTitle = topic.getTitle();
    doc.put("topic_title", StringEscapeUtils.unescapeHtml(topicTitle));
    
    String commentTitle = comment.getTitle();

    if (commentTitle != null &&
        !commentTitle.isEmpty() &&
        !commentTitle.equals(topicTitle) &&
        !commentTitle.startsWith("Re:")) {
      doc.put("title", StringEscapeUtils.unescapeHtml(commentTitle));
    }

    doc.put("message", message);
    Date postdate = comment.getPostdate();
    doc.put("postdate", new Timestamp(postdate.getTime()));

    doc.put("is_comment", true);

    return client
            .prepareIndex(MESSAGES_INDEX, MESSAGES_TYPE, Integer.toString(comment.getId()))
            .setSource(doc);
  }
}
