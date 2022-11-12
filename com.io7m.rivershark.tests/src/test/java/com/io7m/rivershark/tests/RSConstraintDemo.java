/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.rivershark.tests;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.objective.ParetoMaximizer;
import org.chocosolver.solver.variables.IntVar;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RSConstraintDemo
{
  private RSConstraintDemo()
  {

  }

  record Pack(
    String name,
    int major,
    int minor)
  {

  }

  record ModelPack(
    String name,
    IntVar major,
    IntVar minor)
  {

  }

  public static void main(
    final String[] args)
  {
    final var packs = List.of(
      new Pack("a", 1, 0),
      new Pack("a", 1, 1),
      new Pack("a", 1, 2),
      new Pack("a", 1, 3),
      new Pack("b", 1, 0),
      new Pack("b", 2, 0),
      new Pack("b", 2, 1),
      new Pack("b", 2, 2),
      new Pack("b", 3, 0)
    );

    final var modelPacks =
      new HashMap<String, ModelPack>();

    final var model =
      new Model("versions");

    final var versions = new HashMap<String, TreeMap<Integer, TreeSet<Integer>>>();
    for (final var p : packs) {
      final var majors =
        versions.getOrDefault(p.name(), new TreeMap<>());
      final var minors =
        majors.getOrDefault(p.major(), new TreeSet<>());
      minors.add(p.minor());
      majors.put(p.major(), minors);
      versions.put(p.name(), majors);
    }

    for (final var name : versions.keySet()) {
      final var majors = versions.get(name);

      final var varMajor =
        model.intVar(
          "%s.major".formatted(name),
          majors.firstKey().intValue(),
          majors.lastKey().intValue()
        );

      final var minorMin =
        majors.values()
          .stream()
          .flatMap(Collection::stream)
          .min(Integer::compareTo)
          .orElse(0);

      final var minorMax =
        majors.values()
          .stream()
          .flatMap(Collection::stream)
          .max(Integer::compareTo)
          .orElse(0);

      final var varMinor =
        model.intVar("%s.minor".formatted(name),
                     minorMin.intValue(), minorMax.intValue());

      for (final var major : majors.keySet()) {
        final var minors =
          majors.get(major);
        final var minorConstraints =
          new Constraint[minors.size()];

        var index = 0;
        for (final var minor : minors) {
          minorConstraints[index] = model.arithm(varMinor, "=",
                                                 minor.intValue());
          ++index;
        }

        final var majorEq =
          model.arithm(varMajor, "=", major.intValue());
        final var orEq =
          model.or(minorConstraints);

        model.ifThen(majorEq, orEq);
      }

      modelPacks.put(
        name,
        new ModelPack(name, varMajor, varMinor)
      );
    }

    model.arithm(modelPacks.get("b").major, "<", 3).post();

    final var varList =
      modelPacks.values()
        .stream()
        .flatMap(c -> Stream.of(c.major, c.minor))
        .toList();

    final var varArray = new IntVar[varList.size()];
    varList.toArray(varArray);

    final var maximizer = new ParetoMaximizer(varArray);
    final var solver = model.getSolver();
    solver.plugMonitor(maximizer);

    while (solver.solve());

    final var paretoFront = maximizer.getParetoFront();
    System.out.println("The pareto front has " + paretoFront.size() + " solutions : ");
    for (final var s : paretoFront) {
      System.out.println("--");
      for (final var pack : modelPacks.values()) {
        System.out.println(pack.major.getName() +": "+ s.getIntVal(pack.major));
        System.out.println(pack.minor.getName() +": "+s.getIntVal(pack.minor));
      }
    }
  }
}
