/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.muzima.api.db.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.muzima.api.db.DataDao;
import org.openmrs.module.muzima.model.Data;
import org.openmrs.module.muzima.model.handler.DataHandler;
import org.openmrs.util.HandlerUtil;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 */
public abstract class HibernateDataDao<T extends Data> extends HibernateSingleClassDao<T> implements DataDao<T> {

    private final Log log = LogFactory.getLog(HibernateDataDao.class);

    /**
     * Default constructor.
     *
     * @param mappedClass
     */
    protected HibernateDataDao(final Class<T> mappedClass) {
        super(mappedClass);
    }

    /**
     * @return the sessionFactory
     */
    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Return the data with the given id.
     *
     * @param id the data id.
     * @return the data with the matching id.
     * @should return data with matching id.
     * @should return null when no data with matching id.
     */
    @Override
    public T getData(final Integer id) {
        T data = getById(id);
        List<DataHandler> handlers = HandlerUtil.getHandlersForType(DataHandler.class, data.getClass());
        for (DataHandler handler : handlers) {
            if (handler.accept(data)) {
                handler.handleGet(data);
            }
        }
        return data;
    }

    /**
     * Return the data with the given uuid.
     *
     * @param uuid the data uuid.
     * @return the data with the matching uuid.
     * @should return data with matching uuid.
     * @should return null when no data with matching uuid.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T getDataByUuid(final String uuid) {
        Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(mappedClass);
        criteria.add(Restrictions.eq("uuid", uuid));
        T data = (T) criteria.uniqueResult();
        List<DataHandler> handlers = HandlerUtil.getHandlersForType(DataHandler.class, data.getClass());
        for (DataHandler handler : handlers) {
            if (handler.accept(data)) {
                handler.handleGet(data);
            }
        }
        return data;
    }

    /**
     * Return all saved data.
     *
     * @return all saved data.
     */
    @Override
    public List<T> getAllData() {
        List<T> list = new ArrayList<T>();
        for (T data : getAll()) {
            List<DataHandler> handlers = HandlerUtil.getHandlersForType(DataHandler.class, data.getClass());
            for (DataHandler handler : handlers) {
                if (handler.accept(data)) {
                    handler.handleGet(data);
                }
            }
            list.add(data);
        }
        return getAll();
    }

    /**
     * Save data into the database.
     *
     * @param data the data.
     * @return saved data.
     * @should save data into the database.
     */
    @Override
    @Transactional
    public T saveData(final T data) {
        List<DataHandler> handlers = HandlerUtil.getHandlersForType(DataHandler.class, data.getClass());
        for (DataHandler handler : handlers) {
            if (handler.accept(data)) {
                handler.handleSave(data);
            }
        }
        saveOrUpdate(data);
        return data;
    }

    /**
     * Delete data from the database.
     *
     * @param data the data
     * @should remove data from the database
     */
    @Override
    @Transactional
    public void purgeData(final T data) {
        List<DataHandler> handlers = HandlerUtil.getHandlersForType(DataHandler.class, data.getClass());
        for (DataHandler handler : handlers) {
            if (handler.accept(data)) {
                handler.handleDelete(data);
            }
        }
        delete(data);
    }

    /**
     * Get data with matching search term for particular page.
     *
     * @param search     the search term.
     * @param pageNumber the page number.
     * @param pageSize   the size of the page.
     * @return list of data for the page.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<T> getPagedData(final String search, final Integer pageNumber, final Integer pageSize) {
        Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(mappedClass);
        criteria.add(Restrictions.ilike("payload", search, MatchMode.ANYWHERE));
        criteria.setFirstResult((pageNumber - 1) * pageSize);
        criteria.setMaxResults(pageSize);
        return criteria.list();
    }

    /**
     * Get the total number of data with matching search term.
     *
     * @param search the search term.
     * @return total number of data in the database.
     */
    @Override
    public Integer countData(final String search) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(mappedClass);
        criteria.add(Restrictions.ilike("payload", search, MatchMode.ANYWHERE));
        criteria.setProjection(Projections.rowCount());
        return (Integer) criteria.uniqueResult();
    }
}
