/*
 * semanticcms-core-pages-union - Combines multiple sets of SemanticCMS pages.
 * Copyright (C) 2017  AO Industries, Inc.
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
 * along with semanticcms-core-pages-union.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.pages.union;

import com.aoindustries.net.Path;
import com.aoindustries.util.AoCollections;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.PageNotFoundException;
import com.semanticcms.core.pages.Pages;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines multiple sets of SemanticCMS pages.
 */
public class UnionPages implements Pages {

	private static final Map<List<Pages>,UnionPages> unionRepositories = new HashMap<List<Pages>,UnionPages>();

	/**
	 * Gets the union repository representing the given set of repositories, creating a new repository only as-needed.
	 * Only one {@link UnionPages} is created per unique list of underlying repositories.
	 *
	 * @param repositories  A defensive copy is made
	 */
	public static UnionPages getInstance(Pages ... repositories) {
		return getInstance(new ArrayList<Pages>(Arrays.asList(repositories)));
	}

	/**
	 * Gets the union repository representing the given set of repositories, creating a new repository only as-needed.
	 * Only one {@link UnionPages} is created per unique list of underlying repositories.
	 *
	 * @param repositories  Iterated once only.
	 */
	public static UnionPages getInstance(Iterable<Pages> repositories) {
		List<Pages> list = new ArrayList<Pages>();
		for(Pages repository : repositories) list.add(repository);
		return getInstance(list);
	}

	/**
	 * Only one {@link UnionPages} is created per unique list of underlying repositories.
	 */
	private static UnionPages getInstance(List<Pages> stores) {
		if(stores.isEmpty()) throw new IllegalArgumentException("At least one store required");
		synchronized(unionRepositories) {
			UnionPages unionRepository = unionRepositories.get(stores);
			if(unionRepository == null) {
				unionRepository = new UnionPages(stores.toArray(new Pages[stores.size()]));
				unionRepositories.put(stores, unionRepository);
			}
			return unionRepository;
		}
	}

	private final Pages[] repositories;
	private final List<Pages> unmodifiableRepositories;

	private UnionPages(Pages[] repositories) {
		this.repositories = repositories;
		this.unmodifiableRepositories = AoCollections.optimalUnmodifiableList(Arrays.asList(repositories));
	}

	public List<Pages> getRepositories() {
		return unmodifiableRepositories;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("union(");
		for(int i = 0; i < repositories.length; i++) {
			Pages repository = repositories[i];
			if(i > 0) sb.append(", ");
			sb.append(repository.toString());
		}
		return sb.append("):").toString();
	}

	@Override
	public boolean exists(Path path) throws IOException {
		for(Pages repository : repositories) {
			if(repository.exists(path)) return true;
		}
		return false;
	}

	/**
	 * @implSpec  Searches all repositories in-order, returning the first one that {@link Pages#exists(com.aoindustries.net.Path) exists}.
	 *
	 * @throws PageNotFoundException when the page does not exist in any repository
	 */
	@Override
	public Page getPage(Path path, CaptureLevel captureLevel) throws IOException, PageNotFoundException {
		for(Pages repository : repositories) {
			if(repository.exists(path)) {
				// Should we capture PageNotFoundException and try next?
				// This would only make sense if exists is inconsistent with getPage.
				return repository.getPage(path, captureLevel);
			}
		}
		throw new PageNotFoundException(this, path);
	}
}
