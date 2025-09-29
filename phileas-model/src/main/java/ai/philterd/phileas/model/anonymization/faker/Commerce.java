/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright 2014 DiUS Computing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.model.anonymization.faker;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.SortedSet;
import java.util.TreeSet;

public class Commerce {
    private final Faker faker;

    protected Commerce(Faker faker) {
        this.faker = faker;
    }

    public String color() {
        return faker.fakeValuesService().resolve("color.name", this, faker);
    }

    public String department() {
        int numberOfDepartments = Math.max(faker.random().nextInt(4), 1);
        SortedSet<String> departments = new TreeSet<String>();
        while (departments.size() < numberOfDepartments) {
            departments.add(faker.fakeValuesService().resolve("commerce.department", this, faker));
        }
        if (departments.size() > 1) {
            String lastDepartment = departments.last();
            return StringUtils.join(departments.headSet(lastDepartment), ", ") + " & " + lastDepartment;
        } else {
            return departments.first();
        }
    }

    public String productName() {
        return StringUtils.join(new String[] { 
            faker.fakeValuesService().resolve("commerce.product_name.adjective", this,faker), 
            faker.fakeValuesService().resolve("commerce.product_name.material", this, faker),
            faker.fakeValuesService().resolve("commerce.product_name.product", this, faker) }, " ");
    }

    public String material() {
        return faker.fakeValuesService().resolve("commerce.product_name.material", this, faker);
    }

    /**
     * Generate a random price between 0.00 and 100.00 
     */
    public String price() {
        return price(0, 100);
    }

    public String price(double min, double max) {
        double price =  min + (faker.random().nextDouble() * (max - min));
        return new DecimalFormat("#0.00").format(price);
    }

    public String promotionCode() {
        return promotionCode(6);
    }

    public String promotionCode(int digits) {
        return StringUtils.join(faker.resolve("commerce.promotion_code.adjective"),
                faker.resolve("commerce.promotion_code.noun"),
                faker.number().digits(digits));
    }
}
