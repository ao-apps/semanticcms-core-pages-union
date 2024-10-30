/*
 * semanticcms-core-pages-union - Combines multiple sets of SemanticCMS pages.
 * Copyright (C) 2017, 2018, 2019, 2020, 2021, 2022, 2024  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-core-pages-union.
 *
 * semanticcms-core-pages-union is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-core-pages-union is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-core-pages-union.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.semanticcms.core.pages.union;

import com.aoapps.collections.AoCollections;
import com.aoapps.net.Path;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.PageRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines multiple sets of SemanticCMS pages.
 */
public class UnionPageRepository implements PageRepository {

  private static final Map<List<PageRepository>, UnionPageRepository> unionRepositories = new HashMap<>();

  /**
   * Gets the union repository representing the given set of repositories, creating a new repository only as-needed.
   * Only one {@link UnionPageRepository} is created per unique list of underlying repositories.
   *
   * @param repositories  A defensive copy is made
   */
  public static UnionPageRepository getInstance(PageRepository ... repositories) {
    return getInstance(new ArrayList<>(Arrays.asList(repositories)));
  }

  /**
   * Gets the union repository representing the given set of repositories, creating a new repository only as-needed.
   * Only one {@link UnionPageRepository} is created per unique list of underlying repositories.
   *
   * @param repositories  Iterated once only.
   */
  public static UnionPageRepository getInstance(Iterable<PageRepository> repositories) {
    List<PageRepository> list = new ArrayList<>();
    for (PageRepository repository : repositories) {
      list.add(repository);
    }
    return getInstance(list);
  }

  /**
   * Only one {@link UnionPageRepository} is created per unique list of underlying repositories.
   */
  private static UnionPageRepository getInstance(List<PageRepository> stores) {
    if (stores.isEmpty()) {
      throw new IllegalArgumentException("At least one store required");
    }
    synchronized (unionRepositories) {
      UnionPageRepository unionRepository = unionRepositories.get(stores);
      if (unionRepository == null) {
        unionRepository = new UnionPageRepository(stores.toArray(new PageRepository[stores.size()]));
        unionRepositories.put(stores, unionRepository);
      }
      return unionRepository;
    }
  }

  private final PageRepository[] repositories;
  private final List<PageRepository> unmodifiableRepositories;

  private UnionPageRepository(PageRepository[] repositories) {
    this.repositories = repositories;
    this.unmodifiableRepositories = AoCollections.optimalUnmodifiableList(Arrays.asList(repositories));
  }

  @SuppressWarnings("ReturnOfCollectionOrArrayField") // Returning unmodifiable
  public List<PageRepository> getRepositories() {
    return unmodifiableRepositories;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("union(");
    for (int i = 0; i < repositories.length; i++) {
      PageRepository repository = repositories[i];
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(repository.toString());
    }
    return sb.append("):").toString();
  }

  /**
   * Available when all repositories are available.
   */
  @Override
  public boolean isAvailable() {
    for (PageRepository repository : repositories) {
      if (!repository.isAvailable()) {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * <p><b>Implementation Note:</b><br>
   * Searches all repositories in-order, returning the first one that returns non-null from {@link PageRepository#getPage(com.aoapps.net.Path, com.semanticcms.core.pages.CaptureLevel)}.</p>
   *
   * @return  the first page found or {@code null} when the page does not exist in any repository
   */
  @Override
  public Page getPage(Path path, CaptureLevel captureLevel) throws IOException {
    for (PageRepository repository : repositories) {
      Page page = repository.getPage(path, captureLevel);
      if (page != null) {
        return page;
      }
    }
    return null;
  }
}
