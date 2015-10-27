/*
 *     Copyright (c) 2015, NeumimTo https://github.com/NeumimTo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package cz.neumimto.configuration;

import com.typesafe.config.Config;

public interface IMarshaller<T> {
    /**
     * Converts object to String
     *
     * @param t Object which is going to be serialized
     * @return String
     */
    String marshall(T t);

    /**
     * Converts String to object
     *
     * @param string
     * @return
     */
    T unmarshall(Config string);
}
